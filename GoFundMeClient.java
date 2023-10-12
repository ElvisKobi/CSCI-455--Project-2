import java.io.*;
import java.net.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class GoFundMeClient {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                Scanner scanner = new Scanner(System.in)) { // Close the scanner using try-with-resources

            while (true) {
                double targetAmount;
                Date deadline;

                System.out.println("Choose an option:");
                System.out.println("1. Create a new fundraising event");
                System.out.println("2. List fundraising events");
                System.out.println("3. Donate to an event");
                System.out.println("4. Check event details");
                System.out.println("5. Exit");

                int choice = scanner.nextInt();
                switch (choice) {
                    case 1:
                        System.out.println("Enter event name:");
                        scanner.nextLine();
                        String eventName = scanner.nextLine();

                        System.out.println("Enter target amount:");
                        targetAmount = scanner.nextDouble();

                        System.out.println("Enter deadline (in format yyyy-MM-dd):");
                        scanner.nextLine();
                        String deadlineStr = scanner.nextLine();
                        try {
                            deadline = new SimpleDateFormat("yyyy-MM-dd").parse(deadlineStr);
                        } catch (ParseException e) {
                            System.out.println("Invalid date format. Please enter the date in yyyy-MM-dd format.");
                            continue; // Skip to the next iteration of the loop
                        }

                        out.writeUTF("CREATE_EVENT");
                        out.writeUTF(eventName);
                        out.writeDouble(targetAmount);
                        out.writeLong(deadline.getTime());

                        String response = in.readUTF();
                        System.out.println(response);
                        break;

                    case 2:
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
                                name, targetAmount, currentAmount, new SimpleDateFormat("yyyy-MM-dd").format(deadline));
                        break;

                    case 5:
                        System.out.println("Exiting...");
                        return;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
