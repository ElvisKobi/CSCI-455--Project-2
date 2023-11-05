import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/**
 * The GoFundMeServer class represents a server that listens on a specified port for incoming client requests.
 * It logs new client connections and updates the last contact time for each client. It also processes the incoming data and sends a response back to the client.
 * The server supports the following request types:
 * - CREATE_EVENT: creates a new fundraising event
 * - LIST_EVENTS: lists all fundraising events
 * - DONATE: donates to a fundraising event
 * - CHECK_DETAILS: checks the details of a fundraising event
 * - CHECK_EVENTS_EXIST: checks if any fundraising events exist
 * The server also periodically checks for clients that have not contacted the server within the timeout period and removes them from the lastContactMap.
 */
public class GoFundMeServer {

    private static final int PORT = 12345;
    private static List<FundraisingEvent> events = new ArrayList<>();
    private static DatagramSocket serverSocket;
    private static final ConcurrentHashMap<String, Long> lastContactMap = new ConcurrentHashMap<>();
    private static final long TIMEOUT_MILLIS = 30000; // For example, 30 seconds timeout

    /**
     * This method is the main method of the GoFundMeServer class. It starts the server and listens on a specified port for incoming client requests.
     * It logs new client connections and updates the last contact time for each client. It also processes the incoming data and sends a response back to the client.
     * @param args an array of command-line arguments for the server
     */
    public static void main(String[] args) {
        try {
            serverSocket = new DatagramSocket(PORT);
            System.out.println("---------------------------------");
            System.out.println("Server started. Listening on port " + PORT);

            startClientTimeoutChecker();

            byte[] receiveData = new byte[1024];

            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();
                String clientKey = clientAddress.getHostAddress() + ":" + clientPort;

                // Log new client connection
                lastContactMap.computeIfAbsent(clientKey, k -> {
                    System.out.println(getTimestamp() + ": New client connected: IP = " + clientAddress.getHostAddress()
                            + ", Port = " + clientPort);
                    return System.currentTimeMillis();
                });

                // Update last contact time
                lastContactMap.put(clientKey, System.currentTimeMillis());

                byte[] responseData = processData(receivePacket.getData(), clientAddress, clientPort);

                DatagramPacket sendPacket = new DatagramPacket(responseData, responseData.length, clientAddress,
                        clientPort);
                serverSocket.send(sendPacket);
            }
        } catch (BindException e) {
            System.err.println("---------------------------------");
            System.err.println("Could not start the server. Port " + PORT + " is already in use.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts a thread that periodically checks for clients that have not contacted the server within the timeout period.
     * If a client is found to have timed out, it is removed from the lastContactMap and a message is printed to the console.
     */
    private static void startClientTimeoutChecker() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            long currentTime = System.currentTimeMillis();
            Iterator<Map.Entry<String, Long>> iterator = lastContactMap.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<String, Long> entry = iterator.next();
                if (currentTime - entry.getValue() > TIMEOUT_MILLIS) {
                    System.out.println(getTimestamp() + ": Client disconnected: IP = " + entry.getKey().split(":")[0]
                            + ", Port = " + entry.getKey().split(":")[1]);
                    iterator.remove();
                }
            }
        }, TIMEOUT_MILLIS, TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    /**
     * This method processes the incoming data from the client and returns the appropriate response.
     * It reads the request type from the input stream and switches on it to call the corresponding method.
     * If the request type is invalid, it returns an error message.
     * @param data the incoming data from the client
     * @param clientAddress the IP address of the client
     * @param clientPort the port number of the client
     * @return the response to be sent back to the client
     */
    private static byte[] processData(byte[] data, InetAddress clientAddress, int clientPort) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            DataInputStream dis = new DataInputStream(bais);
            String requestType = dis.readUTF();

            System.out.println(getTimestamp() + ": Received request: " + requestType +
                    " from IP = " + clientAddress.getHostAddress() + ", Port = " + clientPort);

            switch (requestType) {
                case "CREATE_EVENT":
                    return createEvent(dis);
                case "LIST_EVENTS":
                    return listEvents();
                case "DONATE":
                    return donate(dis);
                case "CHECK_DETAILS":
                    return checkDetails(dis);
                case "CHECK_EVENTS_EXIST":
                    return checkEventsExist();
                default:
                    return "Invalid request type.".getBytes();
            }
        } catch (IOException e) {
            return "Error processing request.".getBytes();
        }
    }

    
    /**
     * Returns a string representation of the current timestamp in the format "yyyy-MM-dd HH:mm:ss".
     *
     * @return a string representation of the current timestamp
     */
    private static String getTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    /**
     * Creates a new fundraising event with the given name, target amount, and deadline.
     * Adds the new event to the list of events in a synchronized manner.
     * 
     * @param dis the DataInputStream object used to read the name, target amount, and deadline of the event
     * @return a byte array containing the message "Event created successfully."
     * @throws IOException if there is an error reading from the input stream
     */
    private static byte[] createEvent(DataInputStream dis) throws IOException {
        String name = dis.readUTF();
        double targetAmount = dis.readDouble();
        Date deadline = new Date(dis.readLong());

        synchronized (events) {
            FundraisingEvent newEvent = new FundraisingEvent(name, targetAmount, deadline);
            events.add(newEvent);
        }

        return "Event created successfully.".getBytes();
    }

    /**
     * Returns a byte array containing the list of current and past fundraising events.
     * The events are sorted by deadline and the byte array is generated using DataOutputStream.
     * @return a byte array containing the list of current and past fundraising events.
     * @throws IOException if an I/O error occurs.
     */
    private static byte[] listEvents() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
    
        synchronized (events) {
            List<FundraisingEvent> currentEvents = new ArrayList<>();
            List<FundraisingEvent> pastEvents = new ArrayList<>();
    
            for (FundraisingEvent event : events) {
                if (event.deadline.after(new Date())) {
                    currentEvents.add(event);
                } else {
                    pastEvents.add(event);
                }
            }
    
            // Sort the events by deadline
            Comparator<FundraisingEvent> byDeadline = Comparator.comparing(e -> e.deadline);
            currentEvents.sort(byDeadline);
            pastEvents.sort(byDeadline);
    
            dos.writeInt(currentEvents.size());
            dos.writeInt(pastEvents.size());
    
            for (FundraisingEvent event : currentEvents) {
                dos.writeInt(event.id);
                dos.writeUTF(event.name);
                dos.writeDouble(event.targetAmount);
                dos.writeDouble(event.currentAmount);
                dos.writeLong(event.deadline.getTime());
            }
    
            for (FundraisingEvent event : pastEvents) {
                dos.writeInt(event.id);
                dos.writeUTF(event.name);
                dos.writeDouble(event.targetAmount);
                dos.writeDouble(event.currentAmount);
                dos.writeLong(event.deadline.getTime());
            }
        }
    
        return baos.toByteArray();
    }    

    /**
     * This method processes a donation by reading the event index and donation amount from the input stream.
     * It then checks if the event index is valid and if the event deadline has passed. If the event is still active,
     * the donation amount is added to the current amount of the selected event. The method returns a message indicating
     * whether the donation was successful or not.
     *
     * @param dis the input stream to read the event index and donation amount from
     * @return a byte array containing a message indicating whether the donation was successful or not
     * @throws IOException if an I/O error occurs while reading from the input stream
     */
    private static byte[] donate(DataInputStream dis) throws IOException {
        int eventIndex = dis.readInt();
        double donationAmount = dis.readDouble();

        synchronized (events) {
            if (eventIndex < 0 || eventIndex >= events.size()) {
                return "Invalid event index.".getBytes();
            }

            FundraisingEvent selectedEvent = events.get(eventIndex);

            // Check if the event deadline has passed
            if (selectedEvent.deadline.before(new Date())) {
                return "Donation failed. The event has already ended.".getBytes();
            }

            selectedEvent.currentAmount += donationAmount;
        }

        return "Donation successful. Thank you for your contribution!".getBytes();
    }

    /**
     * This method checks the details of a fundraising event based on the event index provided in the DataInputStream.
     * If the event index is invalid, it returns an error message as a byte array.
     * Otherwise, it retrieves the details of the event and writes them to a ByteArrayOutputStream, which is then converted to a byte array and returned.
     *
     * @param dis the DataInputStream containing the event index
     * @return a byte array containing the details of the event, or an error message if the event index is invalid
     * @throws IOException if there is an error reading from the DataInputStream
     */
    private static byte[] checkDetails(DataInputStream dis) throws IOException {
        int eventIndex = dis.readInt();

        synchronized (events) {
            if (eventIndex < 0 || eventIndex >= events.size()) {
                return "Invalid event index.".getBytes();
            }

            FundraisingEvent event = events.get(eventIndex);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeUTF(event.name);
            dos.writeDouble(event.targetAmount);
            dos.writeDouble(event.currentAmount);
            dos.writeLong(event.deadline.getTime());

            return baos.toByteArray();
        }
    }

    /**
     * Represents a fundraising event with an ID, name, target amount, deadline, and current amount raised.
     */
    private static class FundraisingEvent {
        int id;
        String name;
        double targetAmount;
        Date deadline;
        double currentAmount;

        /**
         * Constructs a new fundraising event with the given name, target amount, and deadline.
         * 
         * @param name the name of the fundraising event
         * @param targetAmount the target amount to be raised
         * @param deadline the deadline for the fundraising event
         */
        public FundraisingEvent(String name, double targetAmount, Date deadline) {
            // this.id = events.size() + 1; // Change from events.size() to events.size() +
            // 1
            this.id = events.size();
            this.name = name;
            this.targetAmount = targetAmount;
            this.deadline = deadline;
            this.currentAmount = 0;
        }
    }

    /**
     * Checks if there are any events in the events list and returns a byte array containing a boolean value indicating the result.
     * 
     * @return a byte array containing a boolean value indicating if there are any events in the events list.
     */
    private static byte[] checkEventsExist() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        synchronized (events) {
            try {
                dos.writeBoolean(!events.isEmpty());
            } catch (IOException e) {
                // Handle exception - this should ideally never happen with a
                // ByteArrayOutputStream
            }
        }

        return baos.toByteArray();
    }
}
