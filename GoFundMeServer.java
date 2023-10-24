import java.io.*;
import java.net.*;
import java.util.*;

/**
 * This class represents a server for the GoFundMe application.
 * It listens for incoming client connections and creates a new ClientHandler
 * thread for each client connection.
 * It also contains a nested FundraisingEvent class that represents a
 * fundraising
 * event with a unique ID, name, target amount, deadline, and current amount
 * raised.
 */
public class GoFundMeServer {

    private static final int PORT = 12345;
    private static List<FundraisingEvent> events = new ArrayList<>();

    /**
     * This method starts the GoFundMe server and listens on a specified port for
     * incoming client connections.
     * It creates a new ClientHandler thread for each incoming client connection.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("---------------------------------");
            System.out.println("Server started. Listening on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket).start();
            }
        } catch (BindException e) {
            System.err.println("---------------------------------");
            System.err.println("Could not start the server. Port " + PORT + " is already in use.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Represents a fundraising event with a unique ID, name, target amount,
     * deadline, and current amount raised.
     */
    private static class FundraisingEvent {
        int id;
        String name;
        double targetAmount;
        Date deadline;
        double currentAmount;

        /**
         * Constructor for FundraisingEvent class.
         * 
         * @param name         the name of the fundraising event
         * @param targetAmount the target amount of money to be raised
         * @param deadline     the deadline for the fundraising event
         */
        public FundraisingEvent(String name, double targetAmount, Date deadline) {
            this.id = events.size();
            this.name = name;
            this.targetAmount = targetAmount;
            this.deadline = deadline;
            this.currentAmount = 0;
        }
    }

    /**
     * This class represents a thread that handles incoming client connections.
     * 
     * @throws SocketException if there is an error with the socket connection
     * @throws EOFException    if the end of the file is reached unexpectedly
     * @throws IOException     if there is an error with the input/output streams
     */
    private static class ClientHandler extends Thread {
        private Socket socket;
        private DataInputStream in;
        private DataOutputStream out;

        /**
         * Constructs a new ClientHandler object to handle incoming client connections.
         * 
         * @param socket the socket object representing the client connection
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
         * This method runs the server and listens for incoming requests from clients.
         * It reads the request type from the input stream and processes the request
         * accordingly. It also catches any exceptions that may occur during the process
         * and prints them to the console.
         * 
         * @throws SocketException if there is an error with the socket connection
         * @throws EOFException    if the end of the file is reached unexpectedly
         * @throws IOException     if there is an error with the input/output streams
         */
        @Override
        public void run() {
            try {
                while (true) {
                    String requestType = in.readUTF();
                    System.out.println("Received request: " + requestType + " from IP = "
                            + socket.getInetAddress().getHostAddress() + ", Port = " + socket.getPort());

                    sortEvents();

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

            } catch (SocketException se) {
                Date now = new Date();
                System.out.println(
                        now + ": Client disconnected abruptly: IP = " + socket.getInetAddress().getHostAddress()
                                + ", Port = " + socket.getPort());
            } catch (EOFException eof) {
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
         * Sorts the list of fundraising events by their deadlines in ascending order.
         * Uses a synchronized block to ensure thread safety.
         */
        private void sortEvents() {
            synchronized (events) {
                Collections.sort(events, new Comparator<FundraisingEvent>() {
                    @Override
                    public int compare(FundraisingEvent o1, FundraisingEvent o2) {
                        return o1.deadline.compareTo(o2.deadline);
                    }
                });
            }
        }

        /**
         * Reads input from the client to create a new FundraisingEvent object and adds
         * it to the events list.
         * Sends a success message back to the client.
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

        /**
         * Lists all fundraising events and their details, including id, name, target
         * amount, current amount, and deadline.
         * The events are sorted by their id in ascending order.
         * 
         * @throws IOException if an I/O error occurs while writing to the output
         *                     stream.
         */
        private void listEvents() throws IOException {
            Date now = new Date();
            List<FundraisingEvent> allEvents;
            synchronized (events) {
                allEvents = new ArrayList<>(events);
            }

            out.writeInt(allEvents.size());
            for (FundraisingEvent event : allEvents) {
                out.writeInt(event.id + 1);
                out.writeBoolean(event.deadline.after(now));
                out.writeUTF(event.name);
                out.writeDouble(event.targetAmount);
                out.writeDouble(event.currentAmount);
                out.writeLong(event.deadline.getTime());
            }
        }

        /**
         * Processes a donation for a fundraising event.
         * 
         * @throws IOException if an I/O error occurs
         */
        private void donate() throws IOException {
            int eventIndex = in.readInt();
            double donationAmount = in.readDouble();

            synchronized (events) {
                if (eventIndex < 0 || eventIndex >= events.size()) {
                    out.writeUTF("Invalid event index.");
                    return;
                }

                FundraisingEvent selectedEvent = null;
                // Find the event with the corresponding index
                for (FundraisingEvent event : events) {
                    if (event.id == eventIndex) {
                        selectedEvent = event;
                        break;
                    }
                }

                // If the event is null or has passed, send an error message
                if (selectedEvent == null || selectedEvent.deadline.before(new Date())) {
                    out.writeUTF("Event not found or it has already ended.");
                    return;
                }

                // Process the donation
                selectedEvent.currentAmount += donationAmount;
            }

            out.writeUTF("Donation successful. Thank you for your contribution!");
        }

        /**
         * Checks the details of a fundraising event.
         * 
         * @throws IOException if an I/O error occurs
         */
        private void checkDetails() throws IOException {
            int eventIndex = in.readInt();

            synchronized (events) {
                if (eventIndex < 0 || eventIndex >= events.size()) {
                    out.writeUTF("Invalid event index.");
                    return;
                }

                // Get event by id attribute
                for (FundraisingEvent event : events) {
                    if (event.id == eventIndex) {
                        out.writeUTF(event.name);
                        out.writeDouble(event.targetAmount);
                        out.writeDouble(event.currentAmount);
                        out.writeLong(event.deadline.getTime());
                        return;
                    }
                }
            }
        }
    }
}
