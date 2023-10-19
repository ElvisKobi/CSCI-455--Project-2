import java.io.*;
import java.net.*;
import java.util.*;

/**
 * This class represents a server for a GoFundMe-like application. It listens
 * for incoming client connections and handles requests from clients.
 * The server maintains a list of fundraising events and ensures thread safety
 * when accessing the list.
 * The server can create new fundraising events, list all current and past
 * events, and process donations.
 * The server uses a nested class, FundraisingEvent, to represent a fundraising
 * event with a name, target amount, deadline, and current amount raised.
 * The server uses a nested class, ClientHandler, to handle incoming requests
 * from clients and ensure thread safety when accessing the events list.
 */
public class GoFundMeServer {

    private static final int PORT = 12345;
    private static List<FundraisingEvent> events = new ArrayList<>();

    /**
     * This method is the main method of the GoFundMeServer class. It starts the
     * server and listens on a specific port for incoming client connections.
     * For each incoming client connection, it creates a new ClientHandler thread to
     * handle the client's requests.
     *
     * @param args an array of command-line arguments for the server
     */
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("---------------------------------");
            System.out.println("Server started. Listening on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This class represents a fundraising event with a name, target amount,
     * deadline, and current amount raised.
     */
    private static class FundraisingEvent {
        String name;
        double targetAmount;
        Date deadline;
        double currentAmount;

        /**
         * Constructs a new FundraisingEvent object with the given name, target amount,
         * and deadline.
         * The current amount raised is initialized to 0.
         * 
         * @param name         the name of the fundraising event
         * @param targetAmount the target amount to be raised
         * @param deadline     the deadline for the fundraising event
         */
        public FundraisingEvent(String name, double targetAmount, Date deadline) {
            this.name = name;
            this.targetAmount = targetAmount;
            this.deadline = deadline;
            this.currentAmount = 0;
        }
    }

    /**
     * This class represents a client handler thread that handles requests from
     * clients.
     * It reads the request type from the input stream and calls the corresponding
     * method to handle the request.
     * The class contains methods to create a new fundraising event, list all
     * existing events, process donations,
     * and check the details of a specific event.
     * The methods are synchronized to ensure thread safety when accessing the
     * shared events list.
     * The class also contains a constructor that initializes the input and output
     * streams for the client socket.
     */
    private static class ClientHandler extends Thread {
        private Socket socket;
        private DataInputStream in;
        private DataOutputStream out;

        /**
         * This class represents a client handler for the GoFundMe server. It
         * initializes the input and output streams for the socket connection and prints
         * a message to the console when a new client connects.
         * 
         * @param socket the socket connection to the client
         */
        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());
                // Log current time
                Date now = new Date();
                System.out.println(now + ": New client connected: IP = " + socket.getInetAddress().getHostAddress()
                        + ", Port = " + socket.getPort());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * This method runs the server thread. It listens for incoming requests from
         * clients and processes them accordingly.
         * The method reads the request type from the input stream and uses a switch
         * statement to determine which method to call
         * to process the request. The method catches EOFException and IOException and
         * prints the stack trace. Finally, it closes
         * the socket.
         */
        @Override
        public void run() {
            try {
                while (true) {
                    String requestType = in.readUTF();
                    System.out.println("Received request: " + requestType + " from IP = "
                            + socket.getInetAddress().getHostAddress());

                    switch (requestType) {
                        case "CREATE_EVENT":
                            createEvent();
                            System.out.println("Event created. Responded to client.");
                            break;
                        case "LIST_EVENTS":
                            listEvents();
                            System.out.println("Listed events. Responded to client.");
                            break;
                        case "DONATE":
                            donate();
                            System.out.println("Donation processed. Responded to client.");
                            break;
                        case "CHECK_DETAILS":
                            checkDetails();
                            System.out.println("Checked details. Responded to client.");
                            break;
                        default:
                            out.writeUTF("Invalid request type.");
                            System.out.println("Invalid request. Responded to client.");
                    }
                }
            } catch (EOFException eof) {
                // Log current time
                Date now = new Date();
                System.out.println(
                        now + ": Client disconnected: IP = " + socket.getInetAddress().getHostAddress() + ", Port = "
                                + socket.getPort());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Reads the name, target amount, and deadline of a fundraising event from the
         * input stream and creates a new FundraisingEvent object with the given
         * information.
         * The new event is added to the events list in a synchronized block to ensure
         * thread safety.
         * Sends a success message to the output stream after the event is created.
         * 
         * @throws IOException if there is an error reading from the input stream.
         */
        private void createEvent() throws IOException {
            String name = in.readUTF();
            double targetAmount = in.readDouble();
            Date deadline = new Date(in.readLong());
            synchronized (events) {
                events.add(new FundraisingEvent(name, targetAmount, deadline));
            }
            out.writeUTF("Event created successfully.");
        }

        private void listEvents() throws IOException {
            Date now = new Date();
            List<FundraisingEvent> allEvents;
            synchronized (events) {
                allEvents = new ArrayList<>(events);
            }

            out.writeInt(allEvents.size());
            for (FundraisingEvent event : allEvents) {
                out.writeBoolean(event.deadline.after(now));
                out.writeUTF(event.name);
                out.writeDouble(event.targetAmount);
                out.writeDouble(event.currentAmount);
                out.writeLong(event.deadline.getTime());
            }
        }

        /**
         * This method handles the donation process. It reads the event index and
         * donation amount from the input stream,
         * checks if the event index is valid, adds the donation amount to the
         * corresponding fundraising event's current amount,
         * and writes a success message to the output stream. The method is synchronized
         * to ensure thread safety when accessing
         * the shared events list.
         *
         * @throws IOException if an I/O error occurs while reading from or writing to
         *                     the input/output stream.
         */
        private void donate() throws IOException {
            int eventIndex = in.readInt();
            double donationAmount = in.readDouble();

            synchronized (events) {
                if (eventIndex < 0 || eventIndex >= events.size()) {
                    out.writeUTF("Invalid event index.");
                    return;
                }

                FundraisingEvent event = events.get(eventIndex);
                event.currentAmount += donationAmount;
            }

            out.writeUTF("Donation successful. Thank you for your contribution!");
        }

        /**
         * Reads an integer from the input stream and uses it to retrieve a
         * FundraisingEvent object from the events list.
         * Then, writes the name, target amount, current amount, and deadline of the
         * event to the output stream.
         * If the event index is invalid, writes an error message to the output stream.
         *
         * @throws IOException if an I/O error occurs while reading from or writing to
         *                     the streams.
         */
        private void checkDetails() throws IOException {
            int eventIndex = in.readInt();

            synchronized (events) {
                if (eventIndex < 0 || eventIndex >= events.size()) {
                    out.writeUTF("Invalid event index.");
                    return;
                }

                FundraisingEvent event = events.get(eventIndex);
                out.writeUTF(event.name);
                out.writeDouble(event.targetAmount);
                out.writeDouble(event.currentAmount);
                out.writeLong(event.deadline.getTime());
            }
        }
    }
}
