package org.example;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Serwer {
    private static final int PORT = 8080;
    private static final int MAX_CLIENTS = 5;
    private static final AtomicInteger currentClientCount = new AtomicInteger(0);

    // Mapa przechowująca obiekty
    private static Map<String, List<Object>> obiektyMapa = new HashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Serwer nasłuchuje na porcie " + PORT);

            // Dodanie przykładowych obiekty do mapy
            dodajPrzykladoweObiekty();

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Nowe połączenie przyjęte");

                synchronized (currentClientCount) {
                    // Sprawdzenie czy osiągnięto maksymalną liczbę klientów
                    if (currentClientCount.get() >= MAX_CLIENTS) {
                        System.out.println("Odrzucono połączenie - przekroczono maksymalną liczbę klientów");
                        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
                            out.writeObject("REFUSED");
                            out.flush();
                        } catch (IOException e) {
                            System.err.println("Błąd podczas odrzucania połączenia: " + e.getMessage());
                        } finally {
                            try {
                                socket.close();
                            } catch (IOException e) {
                                System.err.println("Błąd podczas zamykania gniazda: " + e.getMessage());
                            }
                        }
                        continue;
                    }

                    currentClientCount.incrementAndGet();
                    // Obsługa nowego klienta w osobnym wątku
                    new Thread(new ObslugaKlienta(socket)).start();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Metoda dodająca przykładowe obiekty do mapy
    private static void dodajPrzykladoweObiekty() {
        List<Object> koty = Arrays.asList(new Kot("Mruczek"), new Kot("Filemon"));
        List<Object> psy = Arrays.asList(new Pies("Burek"), new Pies("Azor"));
        List<Object> ptaki = Arrays.asList(new Ptak("Wróbel"), new Ptak("Sikorka"));

        obiektyMapa.put("koty", koty);
        obiektyMapa.put("psy", psy);
        obiektyMapa.put("ptaki", ptaki);
    }

    // Metoda pobierająca obiekty z mapy dla podanej klasy
    public static List<Object> getObiektyByKlasa(String className) {
        return obiektyMapa.getOrDefault(className.toLowerCase().replace(" ", ""), new ArrayList<>());
    }

    // Klasa Kot
    static class Kot implements Serializable {
        private String imie;

        public Kot(String imie) {
            this.imie = imie;
        }

        @Override
        public String toString() {
            return "Kot{" +
                    "imie='" + imie + '\'' +
                    '}';
        }
    }

    // Klasa Pies
    static class Pies implements Serializable {
        private String imie;

        public Pies(String imie) {
            this.imie = imie;
        }

        @Override
        public String toString() {
            return "Pies{" +
                    "imie='" + imie + '\'' +
                    '}';
        }
    }

    // Klasa Ptak
    static class Ptak implements Serializable {
        private String gatunek;

        public Ptak(String gatunek) {
            this.gatunek = gatunek;
        }

        @Override
        public String toString() {
            return "Ptak{" +
                    "gatunek='" + gatunek + '\'' +
                    '}';
        }
    }

    // Wątek obsługujący pojedynczego klienta
    static class ObslugaKlienta implements Runnable {
        private Socket socket;
        private int clientId;

        public ObslugaKlienta(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                // Odczytanie ID klienta
                clientId = in.readInt();
                System.out.println("Nowy klient połączony - ID: " + clientId);

                // Wysłanie potwierdzenia połączenia do klienta
                out.writeObject("OK");
                out.flush();

                // Obsługa żądań klienta
                while (true) {
                    try {
                        String request = (String) in.readObject();
                        System.out.println("Klient " + clientId + " wysłał żądanie: " + request);

                        // Obsługa żądania
                        if (request.startsWith("get_")) {
                            String className = request.substring(4).toLowerCase().replace(" ", "");
                            List<Object> objectsToSend = getObiektyByKlasa(className);

                            // Wysłanie kolekcji obiektów do klienta
                            out.writeObject(objectsToSend);
                            out.flush();

                            // Wypisanie na konsoli co zostało przesłane i komu
                            System.out.println("Wysłano kolekcję obiektów klasy " + className + " do klienta " + clientId);
                        } else if (request.equals("exit")) {
                            break; // zakończenie połączenia z klientem
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }

            } catch (IOException e) {
                System.out.println("Błąd podczas obsługi klienta " + clientId + ": " + e.getMessage());
            } finally {
                synchronized (currentClientCount) {
                    currentClientCount.decrementAndGet();
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    System.err.println("Błąd podczas zamykania gniazda klienta " + clientId + ": " + e.getMessage());
                }
            }
        }
    }
}
