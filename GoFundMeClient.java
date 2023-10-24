import java.io.*;
import java.net.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This class implements a client application that communicates with the
 * GoFundMeServer to create fundraising events, list fundraising events, donate
 * to fundraising events, and check fundraising event details.
 */
public class GoFundMeClient {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    /**
     * The main method of the client application.
     * 
     * @param args command line arguments
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        while (true) { // Loop to allow for reconnect attempts
            try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    Scanner scanner = new Scanner(System.in)) {

                int totalEventCountForList = 0;

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
                            System.out.println("---------------------------------");
                            String eventName = getStringInput(scanner, "Enter event name: ");
                            double targetAmount = getDoubleInput(scanner, "Enter target amount: ", 0);
                            Date deadline = getDateInput(scanner, "Enter deadline (in format MM-dd-yyyy): ");

                            out.writeUTF("CREATE_EVENT");
                            out.writeUTF(eventName);
                            out.writeDouble(targetAmount);
                            out.writeLong(deadline.getTime());

                            String response = in.readUTF();
                            System.out.println(response);
                            totalEventCountForList++; // Total event increment
                            break;

                        case 2:
                            System.out.println("---------------------------------");
                            out.writeUTF("LIST_EVENTS");
                            totalEventCountForList = in.readInt();

                            // Check for created events
                            if (totalEventCountForList == 0) {
                                System.out.println("No Event Available. Please create an event first!");
                                break;
                            }

                            List<String> pastEventsOutput = new ArrayList<>();

                            System.out.println("Current Fundraising Events:");
                            for (int i = 1; i <= totalEventCountForList; i++) {
                                int eventIndex = in.readInt();
                                boolean isCurrent = in.readBoolean();
                                eventName = in.readUTF();
                                double eventTargetAmount = in.readDouble();
                                double eventCurrentAmount = in.readDouble();
                                Date eventDeadline = new Date(in.readLong());

                                String output = String.format("%d. %s (Target: $%.2f, Raised: $%.2f, Deadline: %s)\n",
                                        eventIndex, eventName, eventTargetAmount, eventCurrentAmount,
                                        new SimpleDateFormat("MM-dd-yyyy").format(eventDeadline));

                                if (isCurrent) {
                                    System.out.print(output);
                                } else {
                                    pastEventsOutput.add(output);
                                }
                            }

                            System.out.println("\nPast Fundraising Events:");
                            for (String pastEvent : pastEventsOutput) {
                                System.out.print(pastEvent);
                            }
                            break;

                        case 3:
                            // Case 2 in background
                            out.writeUTF("LIST_EVENTS");
                            totalEventCountForList = in.readInt();

                            // Check for created events
                            if (totalEventCountForList == 0) {
                                System.out.println("No Event Available. Please create an event first!");
                                break;
                            }

                            pastEventsOutput = new ArrayList<>();

                            for (int i = 1; i <= totalEventCountForList; i++) {
                                int eventIndex = in.readInt();
                                boolean isCurrent = in.readBoolean();
                                eventName = in.readUTF();
                                double eventTargetAmount = in.readDouble();
                                double eventCurrentAmount = in.readDouble();
                                Date eventDeadline = new Date(in.readLong());

                                String output = String.format("%d. %s (Target: $%.2f, Raised: $%.2f, Deadline: %s)\n",
                                        eventIndex, eventName, eventTargetAmount, eventCurrentAmount,
                                        new SimpleDateFormat("MM-dd-yyyy").format(eventDeadline));

                                if (isCurrent) {
                                    continue;
                                } else {
                                    pastEventsOutput.add(output);
                                }
                            }

                            // Case 3
                            System.out.println("---------------------------------");
                            int eventIndexForDonate = getIntInput(scanner, "Enter event index: ", 1,
                                    totalEventCountForList) - 1; // Use the totalEventCountForList
                            double donationAmount = getDoubleInput(scanner, "Enter donation amount: ", 0);

                            out.writeUTF("DONATE");
                            out.writeInt(eventIndexForDonate);
                            out.writeDouble(donationAmount);

                            String responseForDonate = in.readUTF();
                            System.out.println(responseForDonate);
                            break;

                        case 4:
                            // Case 2 in background
                            out.writeUTF("LIST_EVENTS");
                            totalEventCountForList = in.readInt();

                            // Check for created events
                            if (totalEventCountForList == 0) {
                                System.out.println("No Event Available. Please create an event first!");
                                break;
                            }

                            pastEventsOutput = new ArrayList<>();

                            for (int i = 1; i <= totalEventCountForList; i++) {
                                int eventIndex = in.readInt();
                                boolean isCurrent = in.readBoolean();
                                eventName = in.readUTF();
                                double eventTargetAmount = in.readDouble();
                                double eventCurrentAmount = in.readDouble();
                                Date eventDeadline = new Date(in.readLong());

                                String output = String.format("%d. %s (Target: $%.2f, Raised: $%.2f, Deadline: %s)\n",
                                        eventIndex, eventName, eventTargetAmount, eventCurrentAmount,
                                        new SimpleDateFormat("MM-dd-yyyy").format(eventDeadline));

                                if (isCurrent) {
                                    continue;
                                } else {
                                    pastEventsOutput.add(output);
                                }
                            }

                            // Case 4
                            System.out.println("---------------------------------");
                            int eventIndex4 = getIntInput(scanner, "Enter event index: ", 1, totalEventCountForList)
                                    - 1; // Use the totalEventCountForList

                            out.writeUTF("CHECK_DETAILS");
                            out.writeInt(eventIndex4);

                            String checkEventName = in.readUTF(); // Renamed to avoid conflict
                            double checkTargetAmount = in.readDouble();
                            double checkCurrentAmount = in.readDouble();
                            Date checkDeadline = new Date(in.readLong());
                            System.out.printf(
                                    "Event Details:\nName: %s\nTarget Amount: $%.2f\nAmount Raised: $%.2f\nDeadline: %s\n",
                                    checkEventName, checkTargetAmount, checkCurrentAmount,
                                    new SimpleDateFormat("MM-dd-yyyy").format(checkDeadline));
                            break;

                        case 5:
                            System.out.println("Exiting...");
                            return;
                        default:
                            System.out.println("Invalid choice. Please try again.");
                    }
                }

            } catch (SocketException se) {
                System.out.println("Lost connection to server. Trying to reconnect...");
                // Add a sleep here if you want to introduce a delay before retrying
                Thread.sleep(2000); // 2 seconds delay
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
     * Prompts the user for a date input and returns the value.
     * If the user enters an invalid input, the method will prompt the user to enter
     * a date in the specified format.
     * 
     * @param scanner the Scanner object used to read user input
     * @param prompt  the message to display to the user when prompting for input
     * @return the date entered by the user
     */
    private static Date getDateInput(Scanner scanner, String prompt) {
        System.out.print(prompt);
        Date date = null;
        while (date == null) {
            String input = scanner.nextLine();
            try {
                date = new SimpleDateFormat("MM-dd-yyyy").parse(input);
            } catch (ParseException e) {
                System.out.println("Invalid date format. Please enter the date in MM-dd-yyyy format.");
            }
            if (date == null) {
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
