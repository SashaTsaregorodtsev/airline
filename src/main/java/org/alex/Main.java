package org.alex;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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
             List<Integer> allPrices = new ArrayList<>();
             for (Map.Entry<String, List<Ticket>> entry : groupingCarrier.entrySet()) {
                 String carrier = entry.getKey();
                 List<Ticket> ticketList = entry.getValue();
                 List<Integer> prices = ticketList.stream()
                         .map(t->t.price)
                         .sorted()
                         .collect(Collectors.toList());

                 allPrices.addAll(prices);
                 Duration minFlightTime = ticketList.stream()
                         .map(ticket -> {
                             LocalDateTime departureDate = LocalDateTime.parse(ticket.departure_date + " " + ticket.departure_time, format);
                             LocalDateTime arrivalDate = LocalDateTime.parse(ticket.arrival_date + " " + ticket.arrival_time, format);
                             return Duration.between(departureDate, arrivalDate);
                         })
                         .min(Duration::compareTo)
                         .orElse(Duration.ZERO);

                 ps.println(String.format("Перевозчик: %s%n", carrier));
                 ps.println(String.format("Минимальное время полета: %d ч %d мин%n", minFlightTime.toHours(), minFlightTime.toMinutesPart()));
                 ps.println("----------------------");
             }
            double medianPrice = calculateMedian(allPrices);
            double averagePrice = allPrices.stream().mapToInt(t -> t).average().orElse(0.0);
            double overallDifference = Math.abs(averagePrice - medianPrice);
            ps.println("Разница (средняя цена - медиана): " + overallDifference);
        }catch (IOException e){
            e.printStackTrace();
        }finally {
            ps.close();
        }

    }

    private static double calculateMedian(List<Integer> prices) {
        int size = prices.size();
        if (size == 0) return 0.0;
        Collections.sort(prices);
        if (size % 2 == 1) {
            return prices.get(size / 2);
        } else {
            return (prices.get(size / 2 - 1) + prices.get(size / 2)) / 2.0;
        }
    }
}
