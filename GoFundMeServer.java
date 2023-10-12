import java.io.*;
import java.net.*;
import java.util.*;

public class GoFundMeServer {

    private static final int PORT = 12345;
    private static List<FundraisingEvent> events = new ArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started. Listening on port " + PORT);
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class FundraisingEvent {
        String name;
        double targetAmount;
        Date deadline;
        double currentAmount;

        public FundraisingEvent(String name, double targetAmount, Date deadline) {
            this.name = name;
            this.targetAmount = targetAmount;
            this.deadline = deadline;
            this.currentAmount = 0;
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private DataInputStream in;
        private DataOutputStream out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                String requestType = in.readUTF();

                switch (requestType) {
                    case "CREATE_EVENT":
                        createEvent();
                        break;
                    case "LIST_EVENTS":
                        listEvents();
                        break;
                    case "DONATE":
                        donate();
                        break;
                    case "CHECK_DETAILS":
                        checkDetails();
                        break;
                    default:
                        out.writeUTF("Invalid request type.");
                }
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
            List<FundraisingEvent> currentEvents = new ArrayList<>();
            List<FundraisingEvent> pastEvents = new ArrayList<>();

            Date now = new Date();
            synchronized (events) {
                for (FundraisingEvent event : events) {
                    if (event.deadline.after(now)) {
                        currentEvents.add(event);
                    } else {
                        pastEvents.add(event);
                    }
                }
            }

            out.writeInt(currentEvents.size());
            for (FundraisingEvent event : currentEvents) {
                out.writeUTF(event.name);
                out.writeDouble(event.targetAmount);
                out.writeDouble(event.currentAmount);
                out.writeLong(event.deadline.getTime());
            }

            out.writeInt(pastEvents.size());
            for (FundraisingEvent event : pastEvents) {
                out.writeUTF(event.name);
                out.writeDouble(event.targetAmount);
                out.writeDouble(event.currentAmount);
                out.writeLong(event.deadline.getTime());
            }
        }

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
