package org.example;

import java.io.*;
import java.net.*;

public class Klient {
    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Użycie: java org.example.Klient <host> <port> <clientId>");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        int clientId = Integer.parseInt(args[2]);

        try (Socket socket = new Socket(host, port);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {

            // Wysłanie ID klienta do serwera
            out.writeInt(clientId);
            out.flush();

            // Odczytanie odpowiedzi od serwera (OK lub REFUSED)
            String response = (String) in.readObject();
            if ("REFUSED".equals(response)) {
                System.err.println("Przekroczono maksymalną liczbę klientów. Połączenie odrzucone.");
                return;
            } else {
                System.out.println("Połączono z serwerem. Możesz wysyłać żądania.");
            }

            // Pobieranie opcji od użytkownika i wysyłanie do serwera
            while (true) {
                System.out.println("Wybierz opcję: Koty, Psy, Ptaki lub 'exit' aby zakończyć");
                String option = reader.readLine();

                if (option.equals("exit")) {
                    out.writeObject("exit");
                    out.flush();
                    break;
                }

                if (option.equals("Koty") || option.equals("Psy") || option.equals("Ptaki")) {
                    out.writeObject("get_" + option.substring(0, 1).toUpperCase() + option.substring(1));
                    out.flush();

                    // Odbiór kolekcji obiektów od serwera
                    try {
                        @SuppressWarnings("unchecked")
                        java.util.List<Object> objects = (java.util.List<Object>) in.readObject();
                        System.out.println("Otrzymano kolekcję obiektów " + option + ": " + objects);
                    } catch (ClassNotFoundException e) {
                        System.out.println("Błąd podczas odbierania danych od serwera: " + e.getMessage());
                    }
                } else {
                    System.out.println("Niepoprawna opcja. Spróbuj ponownie.");
                }
            }

        } catch (SocketException e) {
            System.err.println("Połączenie z serwerem zostało przerwane: " + e.getMessage());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
