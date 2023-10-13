import java.io.*;
import java.net.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class GoFundMeClient {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) throws InterruptedException {
        while (true) { // Loop to allow for reconnect attempts
            try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    Scanner scanner = new Scanner(System.in)) {

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
                            Date deadline = getDateInput(scanner, "Enter deadline (in format yyyy-MM-dd): ");

                            out.writeUTF("CREATE_EVENT");
                            out.writeUTF(eventName);
                            out.writeDouble(targetAmount);
                            out.writeLong(deadline.getTime());

                            String response = in.readUTF();
                            System.out.println(response);
                            break;

                        case 2:
                            System.out.println("---------------------------------");
                            out.writeUTF("LIST_EVENTS");

                            int currentEventCount = in.readInt();
                            System.out.println("Current Fundraising Events:");
                            for (int i = 0; i < currentEventCount; i++) {
                                String name = in.readUTF();
                                targetAmount = in.readDouble();
                                double currentAmount = in.readDouble();
                                deadline = new Date(in.readLong());
                                System.out.printf("%d. %s (Target: $%.2f, Raised: $%.2f, Deadline: %s)\n",
                                        i + 1, name, targetAmount, currentAmount,
                                        new SimpleDateFormat("yyyy-MM-dd").format(deadline));
                            }

                            int pastEventCount = in.readInt();
                            System.out.println("\nPast Fundraising Events:");
                            for (int i = 0; i < pastEventCount; i++) {
                                String name = in.readUTF();
                                targetAmount = in.readDouble();
                                double currentAmount = in.readDouble();
                                deadline = new Date(in.readLong());
                                System.out.printf("%d. %s (Target: $%.2f, Raised: $%.2f, Deadline: %s)\n",
                                        i + 1, name, targetAmount, currentAmount,
                                        new SimpleDateFormat("yyyy-MM-dd").format(deadline));
                            }
                            break;

                        case 3:
                            System.out.println("---------------------------------");
                            System.out.println("Enter event index:");
                            int eventIndex = scanner.nextInt() - 1;

                            System.out.println("Enter donation amount:");
                            double donationAmount = scanner.nextDouble();

                            out.writeUTF("DONATE");
                            out.writeInt(eventIndex);
                            out.writeDouble(donationAmount);

                            response = in.readUTF();
                            System.out.println(response);
                            break;

                        case 4:
                            System.out.println("---------------------------------");
                            System.out.println("Enter event index:");
                            eventIndex = scanner.nextInt() - 1;

                            out.writeUTF("CHECK_DETAILS");
                            out.writeInt(eventIndex);

                            String name = in.readUTF();
                            targetAmount = in.readDouble();
                            double currentAmount = in.readDouble();
                            deadline = new Date(in.readLong());
                            System.out.printf(
                                    "Event Details:\nName: %s\nTarget Amount: $%.2f\nAmount Raised: $%.2f\nDeadline: %s\n",
                                    name, targetAmount, currentAmount,
                                    new SimpleDateFormat("yyyy-MM-dd").format(deadline));
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

    private static Date getDateInput(Scanner scanner, String prompt) {
        System.out.print(prompt);
        Date date = null;
        Date today = new Date(); // Gets the current date and time
        while (date == null) {
            String input = scanner.nextLine();
            try {
                date = new SimpleDateFormat("yyyy-MM-dd").parse(input);
                if (!date.after(today)) { // Check if the date is after the current date
                    System.out.println("The date should be in the future. Please enter again.");
                    date = null; // Reset date to null to continue the loop
                }
            } catch (ParseException e) {
                System.out.println("Invalid date format. Please enter the date in yyyy-MM-dd format.");
            }
            if (date == null) { // If date is still null (either due to format error or past date), prompt again
                System.out.print(prompt);
            }
        }
        return date;
    }

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
            System.out.println("Invalid input. Please enter a number between " + min + " and " + max + ".");
            scanner.nextLine(); // Clear the invalid input
        }
        scanner.nextLine(); // Consume the newline character
        return value;
    }
}
