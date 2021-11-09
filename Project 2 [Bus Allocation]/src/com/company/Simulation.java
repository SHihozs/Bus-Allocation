//Yanisa    Suphatsathienkul    6213196
//Phumikorn Sereesantiwong      6213208

package com.company;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.concurrent.CyclicBarrier;

class transactionMisorderedException extends Exception
{
    public transactionMisorderedException(String e)
    {
        super(e);
    }
}

class TicketCounter extends Thread
{
    private int checkPoint;
    private BusLine AirportBusLine;
    private BusLine CityBoundBusLine;
    private Scanner scan;
    private static boolean checkpointReach;
    protected  CyclicBarrier cfinish;
    private int checkTransaction;

    public void setCyclicBarrier(CyclicBarrier f)	{ cfinish = f; }

    public void addCheckTransaction(){ checkTransaction++; }

    public TicketCounter(String name,BusLine AirportBusLine, BusLine CityBoundBusLine,int checkPoint,Scanner scan)
    {
        super(name);
        this.checkPoint = checkPoint;
        this.AirportBusLine = AirportBusLine;
        this.CityBoundBusLine = CityBoundBusLine;
        this.scan = scan;
        this.checkpointReach = false;
        this.checkTransaction = 1;
    }

    public void run()
    {
        while(scan.hasNext())
        {
            try
            {
                String line = scan.nextLine();
                String[] buf= line.split(",");
                if (buf.length < 4 || buf.length > 4)
                {
                    throw new ArrayIndexOutOfBoundsException(line);
                }

                readFile(line,buf);
            }
            catch (ArrayIndexOutOfBoundsException e)
            {
                System.out.println("\n[ ARRAY INDEX OUT OF BOUNDS : " + Thread.currentThread().getName() + " >> " + e.getMessage() + " ]");
                System.exit(-1);
            }
        }
    }

    public void readFile(String line,String[] buf)
    {
        try
        {
            int transactionID = Integer.parseInt(buf[0].trim());
            String tourGroupName = buf[1].trim();
            int passenger = Integer.parseInt(buf[2].trim());
            String destination = buf[3].toUpperCase().trim();

            if(passenger < 1){ throw new InputMismatchException(); }

            if(transactionID != checkTransaction) { throw new transactionMisorderedException("TRANSACTION IS NOT IN ORDERED"); }

            try { cfinish.await(); } catch (Exception e) { }
            if(transactionID == checkPoint && checkpointReach == false)
            {
                checkpointReach = true;
                System.out.println();
                System.out.println("=============================[CHECKPOINT]=============================");
                AirportBusLine.printCheckpoint("Airport-Bound");
                CityBoundBusLine.printCheckpoint("City-Bound");
                System.out.println("======================================================================");
                System.out.println();
            }

            try { cfinish.await(); } catch (Exception e) { }
            if(destination.equals("A"))
            {
                AirportBusLine.allocateBus(transactionID,tourGroupName,passenger);
            }
            else if(destination.equals("C"))
            {
                CityBoundBusLine.allocateBus(transactionID,tourGroupName,passenger);
            }
            else
            {
                throw new InputMismatchException();
            }

            addCheckTransaction();
        }
        catch (NumberFormatException e)
        {
            System.out.println("\n[ NUMBER FORMAT EXCEPTION : " + Thread.currentThread().getName() + " >> " + line + " ]");
            System.exit(-1);
        }
        catch (InputMismatchException e)
        {
            System.out.println("\n[ INPUT MISMATCH EXCEPTION : " + Thread.currentThread().getName() + " >> " + line + " ]");
            System.exit(-1);
        }
        catch (transactionMisorderedException e)
        {
            System.out.println("\n[ " + e.getMessage() + " : " + Thread.currentThread().getName() + " >> " + line + " ]");
            System.exit(-1);
        }
    }
}

class Group
{
    private String tourGroupName;
    private int tourist;

    public String getTourGroupName() { return tourGroupName; }

    public int getTourist() { return tourist; }

    public Group(String name,int tourist)
    {
        this.tourGroupName = name;
        this.tourist = tourist;
    }
}

class Bus
{
    private String busName;
    private ArrayList<Group> tourGroup;
    private int passenger;

    public void addPassenger(int passenger)
    {
        this.passenger += passenger;
    }

    public int getPassenger() { return passenger; }

    public String getBusName() { return busName; }

    public Bus(String busName)
    {
        this.busName = busName;
        this.tourGroup = new ArrayList<>();
        this.passenger = 0;
    }

    public void addTourGroup(String tourGroupName,int passenger)
    {
        Group g = new Group(tourGroupName,passenger);
        this.tourGroup.add(g);
    }

    public void print()
    {
        System.out.printf("%s >> %-3s : ",Thread.currentThread().getName(),busName);

        StringBuilder result = new StringBuilder();

        for (Group p : tourGroup)
        {
            result.append(String.format("%-20s ", p.getTourGroupName()) + "(" + String.format("%-2d", p.getTourist()) + " Seats)").append(", ");
        }

        String withoutLastComma = result.substring( 0, result.length( ) - ", ".length());
        System.out.println(withoutLastComma);
    }
}

class BusLine
{
    private Character destination;
    private int maximum_seats;
    private int busNumber;
    private ArrayList<Bus> bus;

    public ArrayList<Bus> getBus() { return bus; }

    public BusLine(Character destination,int maximum_seats)
    {
        this.destination = destination;
        this.maximum_seats = maximum_seats;
        this.busNumber = 0;
        this.bus = new ArrayList<>();
    }

    public void addBus(String busName)
    {
        Bus b = new Bus(busName);
        bus.add(b);
    }

    public synchronized void allocateBus(int transactionID,String tourGroupName,int passenger)
    {
        if(bus.size()==0)
        {
            addBus(destination + "" + busNumber);
        }

        while(passenger!=0)
        {
            if ((maximum_seats - bus.get(busNumber).getPassenger()) >= passenger)
            {
                bus.get(busNumber).addTourGroup(tourGroupName,passenger);
                bus.get(busNumber).addPassenger(passenger);
                System.out.printf("%s >> Transaction  %-2d : %-20s (%-2d seats) Bus %s\n", Thread.currentThread().getName(), transactionID, tourGroupName, passenger, bus.get(busNumber).getBusName());
                passenger = 0;
            }
            else
            {
                passenger = passenger - (maximum_seats - bus.get(busNumber).getPassenger());

                if(maximum_seats - bus.get(busNumber).getPassenger()!=0)
                {
                    System.out.printf("%s >> Transaction  %-2d : %-20s (%-2d seats) Bus %s\n", Thread.currentThread().getName(), transactionID, tourGroupName, (maximum_seats - bus.get(busNumber).getPassenger()), bus.get(busNumber).getBusName());
                    bus.get(busNumber).addTourGroup(tourGroupName,maximum_seats - bus.get(busNumber).getPassenger());
                    bus.get(busNumber).addPassenger(maximum_seats - bus.get(busNumber).getPassenger());
                }

                busNumber++;
                addBus(destination + "" + busNumber);
            }
        }
    }

    public void printCheckpoint(String busName)
    {
        //========== Check Grammar ==========//
        if(bus.size()<2)
        {
            System.out.printf("%s >> %d %s bus has been allocated \n",Thread.currentThread().getName(),bus.size(),busName);
        }
        else
        {
            System.out.printf("%s >> %d %s buses have been allocated \n",Thread.currentThread().getName(),bus.size(),busName);
        }
    }
}

public class Simulation
{
    public int enterMaxSeats()
    {
        int maximumSeat = 0;
        boolean redo = true;
        while(redo)
        {
            try
            {
                Scanner scan = new Scanner(System.in);
                System.out.println(Thread.currentThread().getName() + " >> Enter Max Seats = ");
                maximumSeat = scan.nextInt();
                if(maximumSeat < 1) { throw new InputMismatchException("MAXIMUM SEATS LESS THAN 1"); }
                redo = false;
                scan.reset();
            }
            catch (InputMismatchException e)
            {
                System.out.println("\n[ INPUT MISMATCH EXCEPTION : " + e.getMessage() + " ]\n");
            }
        }
        return maximumSeat;
    }

    public int enterCheckpoint()
    {
        int checkpoint = 0;
        boolean redo = true;
        while(redo)
        {
            try
            {
                Scanner scan = new Scanner(System.in);
                System.out.println(Thread.currentThread().getName() + " >> Enter Checkpoint = ");
                checkpoint = scan.nextInt();
                if(checkpoint < 1){ throw new InputMismatchException("Checkpoint < 1"); }
                if(checkpoint > 10){ throw new InputMismatchException("Checkpoint > 10"); }
                redo = false;
                scan.reset();
                scan.close();
            }
            catch (InputMismatchException e)
            {
                System.out.println("\n[ INPUT MISMATCH EXCEPTION : " + e.getMessage() + " ]\n");
            }
        }
        return checkpoint;
    }

    public void printSummary(ArrayList<Bus> b)
    {
        for (Bus p : b)
        {
            p.print();
        }
    }

    public static void main(String[] args)
    {
        Simulation simulation = new Simulation();

        //==================== INPUT ====================//

        System.out.println("======================================================================");

        int maximumSeat = simulation.enterMaxSeats();
        int checkpoint = simulation.enterCheckpoint();

        System.out.println("======================================================================");

        BusLine AirportBusLine = new BusLine('A',maximumSeat);
        BusLine CityBoundBusLine = new BusLine('C',maximumSeat);

        //==================== PROCESS ====================//

        ArrayList<TicketCounter> tc = new ArrayList<>();

        CyclicBarrier finish = new CyclicBarrier(3);

        for (int i=0;i<3;i++)
        {
            try
            {
                Scanner scan = new Scanner(new File("T" + (i+1) + ".txt"));
                TicketCounter t = new TicketCounter("T"+(i+1),AirportBusLine,CityBoundBusLine,checkpoint,scan);
                t.setCyclicBarrier(finish);
                tc.add(t);
            }
            catch (FileNotFoundException e)
            {
                System.out.println("\n[ FILE NOT FOUND : " + e.getMessage() + " ]");
                System.exit(-1);
            }
        }

        for (TicketCounter p : tc)
        {
            p.start();
        }

        try
        {
            for (TicketCounter p : tc)
            {
                p.join();
            }
        }
        catch (InterruptedException e) { }

        //==================== SUMMARY ====================//

        System.out.printf("\n");
        System.out.printf("%s >> ================================================================= [AIRPORT BOUND] ================================================================= \n",Thread.currentThread().getName());

        simulation.printSummary(AirportBusLine.getBus());

        System.out.printf("%s >> =================================================================================================================================================== \n\n",Thread.currentThread().getName());

        System.out.printf("%s >> ================================================================= [CITY    BOUND] ================================================================= \n",Thread.currentThread().getName());

        simulation.printSummary(CityBoundBusLine.getBus());

        System.out.printf("%s >> =================================================================================================================================================== \n\n",Thread.currentThread().getName());
    }
}
