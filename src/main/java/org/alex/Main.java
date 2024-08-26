package org.alex;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class Ticket{
    public String origin;
    public String destination;
    public String origin_name;
    public String from;
    public String destination_name;
    public String departure_date;
    public String departure_time;
    public String arrival_date;
    public String arrival_time;
    public String carrier;
    public int stops;
    public String status;
    public int price;
}

class Tickets{
    public List<Ticket> tickets;
}

public class Main {
    public static void main(String[] args) throws FileNotFoundException {
        ObjectMapper om = new ObjectMapper();
        PrintStream ps = new PrintStream("result_flights_statistics.txt");
        DateTimeFormatter format = DateTimeFormatter.ofPattern("dd.MM.yy H:mm");
        var from = "Владивосток";
        var to = "Тель-Авив";

        try{
             Tickets tickets = om.readValue(new File("src/main/resources/tickets.json"), new TypeReference<>() {});
             List<Ticket> formatTickets = tickets.tickets.stream().filter(t -> t.origin_name.equals(from) && t.destination_name.equals(to)).toList();
             Map<String,List<Ticket>> groupingCarrier = formatTickets.stream().collect(Collectors.groupingBy(t -> t.carrier));
             ps.println(String.format("Группировка по перевозчикам: %s - %s",from,to));
             ps.println("----------------------");
             for (Map.Entry<String, List<Ticket>> entry : groupingCarrier.entrySet()) {
                 String carrier = entry.getKey();
                 List<Ticket> ticketList = entry.getValue();
                 List<Integer> prices = ticketList.stream()
                         .map(t->t.price)
                         .sorted()
                         .collect(Collectors.toList());

                 double medianPrice = calculateMedian(prices);

                 double averagePrice = ticketList.stream().mapToInt(t -> t.price).average().orElse(0.0);
                 double difference = averagePrice - medianPrice;

                 Duration minFlightTime = ticketList.stream()
                         .map(ticket -> {
                             LocalDateTime departureDate = LocalDateTime.parse(ticket.departure_date + " " + ticket.departure_time, format);
                             LocalDateTime arrivalDate = LocalDateTime.parse(ticket.arrival_date + " " + ticket.arrival_time, format);
                             return Duration.between(departureDate, arrivalDate);
                         })
                         .min(Duration::compareTo)
                         .orElse(Duration.ZERO);

                 ps.println(String.format("Перевозчик: %s%n", carrier));
                 ps.println(String.format("Разница: %.2f%n", difference));
                 ps.println(String.format("Минимальное время полета: %d ч %d мин%n", minFlightTime.toHours(), minFlightTime.toMinutesPart()));
                 ps.println("----------------------");
             }
             ps.close();

        }catch (IOException e){
            e.printStackTrace();
        }

    }

    private static double calculateMedian(List<Integer> prices) {
        int size = prices.size();
        if (size == 0) return 0.0;

        if (size % 2 == 1) {
            return prices.get(size / 2);
        } else {
            return (prices.get(size / 2 - 1) + prices.get(size / 2)) / 2.0;
        }
    }
}
