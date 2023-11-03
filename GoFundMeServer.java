import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class GoFundMeServer {

    private static final int PORT = 12345;
    private static List<FundraisingEvent> events = new ArrayList<>();
    private static DatagramSocket serverSocket;
    private static final ConcurrentHashMap<String, Long> lastContactMap = new ConcurrentHashMap<>();
    private static final long TIMEOUT_MILLIS = 30000; // For example, 30 seconds timeout

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

    // Helper method to get current timestamp for logging
    private static String getTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

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

    private static byte[] listEvents() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        synchronized (events) {
            dos.writeInt(events.size());
            for (FundraisingEvent event : events) {
                dos.writeInt(event.id);
                dos.writeUTF(event.name);
                dos.writeDouble(event.targetAmount);
                dos.writeDouble(event.currentAmount);
                dos.writeLong(event.deadline.getTime());
            }
        }

        return baos.toByteArray();
    }

    private static byte[] donate(DataInputStream dis) throws IOException {
        int eventIndex = dis.readInt();
        double donationAmount = dis.readDouble();

        synchronized (events) {
            if (eventIndex < 0 || eventIndex >= events.size()) {
                return "Invalid event index.".getBytes();
            }

            FundraisingEvent selectedEvent = events.get(eventIndex);
            selectedEvent.currentAmount += donationAmount;
        }

        return "Donation successful. Thank you for your contribution!".getBytes();
    }

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

    private static class FundraisingEvent {
        int id;
        String name;
        double targetAmount;
        Date deadline;
        double currentAmount;

        public FundraisingEvent(String name, double targetAmount, Date deadline) {
            this.id = events.size();
            this.name = name;
            this.targetAmount = targetAmount;
            this.deadline = deadline;
            this.currentAmount = 0;
        }
    }

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
