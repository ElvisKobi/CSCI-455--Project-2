import java.io.*;
import java.net.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * The GoFundMeClient class is a client program that allows users to create, list, and donate to fundraising events.
 * It connects to a server using a DatagramSocket and sends requests to the server to perform actions.
 * The class contains methods for creating a new event, listing all events, donating to an event, and checking event details.
 * The main method of the class prompts the user to choose an option from a menu and performs the corresponding action based on the user's choice.
 * The class also contains helper methods for sending and receiving datagram packets to and from the server.
 */
public class GoFundMeClient {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    private static DatagramSocket clientSocket;
    private static InetAddress serverAddress;

    /**
     * This method is the main method of the GoFundMeClient class. It creates a DatagramSocket and connects to the server. 
     * It then prompts the user to choose an option from a menu and performs the corresponding action based on the user's choice.
     * The options include creating a new fundraising event, listing fundraising events, donating to an event, checking event details, and exiting the program.
     * 
     * @param args an array of command-line arguments for the program
     * @throws InterruptedException if the thread is interrupted while sleeping
     */
    public static void main(String[] args) throws InterruptedException {
        try {
            clientSocket = new DatagramSocket();
            serverAddress = InetAddress.getByName(SERVER_ADDRESS);
            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.println("---------------------------------");
                System.out.println("Choose an option:");
                System.out.println("1. Create a new fundraising event");
                System.out.println("2. List fundraising events");
                System.out.println("3. Donate to an event");
                System.out.println("4. Check event details");
                System.out.println("5. Exit");

                int choice = getIntInput(scanner, "Enter your choice: ", 1, 5);

                switch (choice) {
                    case 1:
                        createEvent(scanner);
                        break;
                    case 2:
                        listEvents();
                        break;
                    case 3:
                        donate(scanner);
                        break;
                    case 4:
                        checkDetails(scanner);
                        break;
                    case 5:
                        System.out.println("Exiting...");
                        clientSocket.close();
                        return;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            }
        } catch (UnknownHostException e) {
            System.err.println("Server not found: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("I/O Error: " + e.getMessage());
            Thread.sleep(2000); // 2 seconds delay before retrying
        }
    }

    /**
     * Sends a datagram packet to the server with the specified data.
     * 
     * @param sendData the data to be sent in the packet
     * @throws IOException if an I/O error occurs while sending the packet
     */
    private static void sendRequest(byte[] sendData) throws IOException {
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, SERVER_PORT);
        clientSocket.send(sendPacket);
    }

    /**
     * Receives a response from the server.
     * @return the response received from the server as a String.
     * @throws IOException if an I/O error occurs.
     */
    private static String receiveResponse() throws IOException {
        byte[] receiveData = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        clientSocket.receive(receivePacket);
        return new String(receivePacket.getData(), 0, receivePacket.getLength());
    }

    /**
     * Creates a new fundraising event by prompting the user to input the event name, target amount, and deadline.
     * Then, it sends a request to the server with the event information and receives a response.
     * @param scanner a Scanner object used to read user input
     * @throws IOException if an I/O error occurs
     */
    private static void createEvent(Scanner scanner) throws IOException {
        System.out.println("---------------------------------");

        String eventName = getStringInput(scanner, "Enter event name: ");
        double targetAmount = getDoubleInput(scanner, "Enter target amount: ", 0);
        Date deadline = getDateInput(scanner, "Enter deadline (in format MM-dd-yyyy): ");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeUTF("CREATE_EVENT");
        dos.writeUTF(eventName);
        dos.writeDouble(targetAmount);
        dos.writeLong(deadline.getTime());

        sendRequest(baos.toByteArray());
        String response = receiveResponse();
        System.out.println(response);
    }

    /**
     * Sends a request to the server to list all fundraising events and prints the details of each event.
     * The method sends a "LIST_EVENTS" message to the server and receives the response data.
     * The response data contains the number of current and past events, followed by the details of each event.
     * If there are no current or past events, the method prints a message indicating so.
     * @throws IOException if an I/O error occurs while sending or receiving data.
     */
    private static void listEvents() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeUTF("LIST_EVENTS");

        sendRequest(baos.toByteArray());

        byte[] responseData = receiveResponseData();
        ByteArrayInputStream bais = new ByteArrayInputStream(responseData);
        DataInputStream dis = new DataInputStream(bais);

        int numberOfCurrentEvents = dis.readInt();
        int numberOfPastEvents = dis.readInt();

        System.out.println("---------------------------------");

        System.out.println("Current Events:");
        if (numberOfCurrentEvents == 0) {
            System.out.println("There are currently no ongoing fundraising events.");
        } else {
            for (int i = 0; i < numberOfCurrentEvents; i++) {
                printEventDetails(dis);
            }
        }

        System.out.println("\nPast Events:");
        if (numberOfPastEvents == 0) {
            System.out.println("There are no past fundraising events.");
        } else {
            for (int i = 0; i < numberOfPastEvents; i++) {
                printEventDetails(dis);
            }
        }
    }

    /**
     * Reads and prints the details of an event from the given DataInputStream.
     * The details include the event ID, name, target amount, current amount raised, and deadline.
     * 
     * @param dis the DataInputStream to read the event details from
     * @throws IOException if there is an error reading from the DataInputStream
     */
    private static void printEventDetails(DataInputStream dis) throws IOException {
        int id = dis.readInt();
        String name = dis.readUTF();
        double targetAmount = dis.readDouble();
        double currentAmount = dis.readDouble();
        long deadlineMillis = dis.readLong();
        Date deadline = new Date(deadlineMillis);

        // Display the id directly as it already starts from 1
        System.out.printf("%d: %s (Target: $%.2f, Raised: $%.2f, Deadline: %s)\n",
                id + 1, name, targetAmount, currentAmount, deadline.toString());
    }

    /**
     * Receives response data from the server.
     * @return the received data as a byte array
     * @throws IOException if an I/O error occurs
     */
    private static byte[] receiveResponseData() throws IOException {
        byte[] receiveData = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        clientSocket.receive(receivePacket);
        return Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
    }

    /**
     * Allows the user to donate to a fundraising event if there are any available.
     * Prompts the user to enter the index of the event they want to donate to and the amount they want to donate.
     * Sends a request to the server with the event index and donation amount.
     * Receives and prints the response from the server.
     * If there are no fundraising events available, prints a message indicating so.
     *
     * @param scanner a Scanner object used to get user input
     * @throws IOException if there is an error with the input/output streams
     */
    private static void donate(Scanner scanner) throws IOException {
        if (checkIfEventsExist()) {
            System.out.println("---------------------------------");

            int eventIndex = getIntInput(scanner, "Enter event index: ", 0, Integer.MAX_VALUE);
            double donationAmount = getDoubleInput(scanner, "Enter donation amount: ", 0);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeUTF("DONATE");
            dos.writeInt(eventIndex - 1);
            dos.writeDouble(donationAmount);

            sendRequest(baos.toByteArray());
            String response = receiveResponse();
            System.out.println(response);
        } else {
            System.out.println("There are currently no fundraising events to donate to.");
        }
    }

    /**
     * This method checks the details of a fundraising event by prompting the user to enter the event index.
     * If the event exists, it sends a request to the server to retrieve the event details and displays them to the user.
     * Otherwise, it informs the user that there are no fundraising events to check details for.
     *
     * @param scanner a Scanner object used to read user input
     * @throws IOException if an I/O error occurs while sending or receiving data from the server
     */
    private static void checkDetails(Scanner scanner) throws IOException {
        if (checkIfEventsExist()) {
            System.out.println("---------------------------------");

            int eventIndex = getIntInput(scanner, "Enter event index: ", 0, Integer.MAX_VALUE);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeUTF("CHECK_DETAILS");
            dos.writeInt(eventIndex - 1);

            sendRequest(baos.toByteArray());
            byte[] responseData = receiveResponseData();
            ByteArrayInputStream bais = new ByteArrayInputStream(responseData);
            DataInputStream dis = new DataInputStream(bais);

            String name = dis.readUTF();
            double targetAmount = dis.readDouble();
            double currentAmount = dis.readDouble();
            long deadlineMillis = dis.readLong();
            Date deadline = new Date(deadlineMillis);

            System.out.printf("Name: %s\nTarget Amount: %.2f\nCurrent Amount: %.2f\nDeadline: %s\n",
                    name, targetAmount, currentAmount, deadline.toString());
        } else {
            System.out.println("There are currently no fundraising events to check details for.");
        }
    }

    private static boolean checkIfEventsExist() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeUTF("CHECK_EVENTS_EXIST");

        sendRequest(baos.toByteArray());
        byte[] responseData = receiveResponseData();
        ByteArrayInputStream bais = new ByteArrayInputStream(responseData);
        DataInputStream dis = new DataInputStream(bais);

        return dis.readBoolean();
    }

    /**
     * Prompts the user for a string input and returns the value.
     * If the user enters an empty string, the method will prompt the user to enter
     * a non-empty string.
     * 
     * @param scanner the Scanner object used to read user input
     * @param prompt  the message to display to the user when prompting for input
     * @return the string entered by the user
     */
    private static String getStringInput(Scanner scanner, String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        while (input.isEmpty()) {
            System.out.println("Input cannot be empty. Please try again.");
            System.out.print(prompt);
            input = scanner.nextLine().trim();
        }
        return input;
    }

    /**
     * Prompts the user for a double input and returns the value.
     * If the user enters an invalid input, the method will prompt the user to enter
     * a positive number.
     * 
     * @param scanner  the Scanner object used to read user input
     * @param prompt   the message to display to the user when prompting for input
     * @param minValue the minimum value of the input (exclusive)
     * @return the double value entered by the user
     */
    private static double getDoubleInput(Scanner scanner, String prompt, double minValue) {
        double value;
        while (true) {
            System.out.print(prompt);
            if (scanner.hasNextDouble()) {
                value = scanner.nextDouble();
                if (value > minValue) {
                    break;
                }
            }
            System.out.println("Invalid input. Please enter a positive number.");
            scanner.nextLine(); // Clear the invalid input
        }
        scanner.nextLine(); // Consume the newline character
        return value;
    }

    /**
     * Prompts the user to enter a date in MM-dd-yyyy format and returns a Date
     * object.
     * If the user enters an invalid date, the method will continue to prompt the
     * user until a valid date is entered.
     * 
     * @param scanner the Scanner object used to read user input
     * @param prompt  the prompt to display to the user
     * @return a Date object representing the date entered by the user
     */
    private static Date getDateInput(Scanner scanner, String prompt) {
        System.out.print(prompt);
        Date date = null;
        while (date == null) {
            String input = scanner.nextLine();
            String[] dateParts = input.split("-");

            if (dateParts.length != 3) {
                System.out.println("Invalid format. Please enter the date in MM-dd-yyyy format.");
                System.out.print(prompt);
                continue;
            }

            try {
                int month = Integer.parseInt(dateParts[0]);
                int day = Integer.parseInt(dateParts[1]);
                int year = Integer.parseInt(dateParts[2]);

                if (month < 1 || month > 12 || day < 1 || day > 31 || year <= 0) {
                    throw new NumberFormatException();
                }

                DateFormat df = new SimpleDateFormat("MM-dd-yyyy");
                df.setLenient(false);
                date = df.parse(input);

            } catch (NumberFormatException | ParseException e) {
                System.out.println("Invalid date. ");
                System.out.print(prompt);
            }
        }
        return date;
    }

    /**
     * This method prompts the user to enter an integer value within a specified
     * range and returns the value.
     * If the user enters an invalid input, the method will prompt the user to enter
     * a number between the specified range.
     * 
     * @param scanner the Scanner object used to read user input
     * @param prompt  the message to prompt the user for input
     * @param min     the minimum value of the range (inclusive)
     * @param max     the maximum value of the range (inclusive)
     * @return the integer value entered by the user within the specified range
     */
    private static int getIntInput(Scanner scanner, String prompt, int min, int max) {
        int value;
        while (true) {
            System.out.print(prompt);
            if (scanner.hasNextInt()) {
                value = scanner.nextInt();
                if (value >= min && value <= max) {
                    break;
                }
            }
            // case where min and max are the same
            if (min == max) {
                System.out.println("Invalid input. Please enter " + min + ".");
            } else {
                System.out.println("Invalid input. Please enter a number between " + min + " and " + max + ".");
            }
            scanner.nextLine(); // Clear the invalid input
        }
        scanner.nextLine(); // Consume the newline character
        return value;
    }
}
