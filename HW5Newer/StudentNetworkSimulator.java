import java.util.*;
import java.io.*;

import java.util.ArrayList; // IMPORRTED ARRAYLIST //

public class StudentNetworkSimulator extends NetworkSimulator
{
    /*
     * Predefined Constants (static member variables):
     *
     *   int MAXDATASIZE : the maximum size of the Message data and
     *                     Packet payload
     *
     *   int A           : a predefined integer that represents entity A
     *   int B           : a predefined integer that represents entity B 
     *
     * Predefined Member Methods:
     *
     *  void stopTimer(int entity): 
     *       Stops the timer running at "entity" [A or B]
     *  void startTimer(int entity, double increment): 
     *       Starts a timer running at "entity" [A or B], which will expire in
     *       "increment" time units, causing the interrupt handler to be
     *       called.  You should only call this with A.
     *  void toLayer3(int callingEntity, Packet p)
     *       Puts the packet "p" into the network from "callingEntity" [A or B]
     *  void toLayer5(String dataSent)
     *       Passes "dataSent" up to layer 5
     *  double getTime()
     *       Returns the current time in the simulator.  Might be useful for
     *       debugging.
     *  int getTraceLevel()
     *       Returns TraceLevel
     *  void printEventList()
     *       Prints the current event list to stdout.  Might be useful for
     *       debugging, but probably not.
     *
     *
     *  Predefined Classes:
     *
     *  Message: Used to encapsulate a message coming from layer 5
     *    Constructor:
     *      Message(String inputData): 
     *          creates a new Message containing "inputData"
     *    Methods:
     *      boolean setData(String inputData):
     *          sets an existing Message's data to "inputData"
     *          returns true on success, false otherwise
     *      String getData():
     *          returns the data contained in the message
     *  Packet: Used to encapsulate a packet
     *    Constructors:
     *      Packet (Packet p):
     *          creates a new Packet that is a copy of "p"
     *      Packet (int seq, int ack, int check, String newPayload)
     *          creates a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and a
     *          payload of "newPayload"
     *      Packet (int seq, int ack, int check)
     *          chreate a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and
     *          an empty payload
     *    Methods:
     *      boolean setSeqnum(int n)
     *          sets the Packet's sequence field to "n"
     *          returns true on success, false otherwise
     *      boolean setAcknum(int n)
     *          sets the Packet's ack field to "n"
     *          returns true on success, false otherwise
     *      boolean setChecksum(int n)
     *          sets the Packet's checksum to "n"
     *          returns true on success, false otherwise
     *      boolean setPayload(String newPayload)
     *          sets the Packet's payload to "newPayload"
     *          returns true on success, false otherwise
     *      int getSeqnum()
     *          returns the contents of the Packet's sequence field
     *      int getAcknum()
     *          returns the contents of the Packet's ack field
     *      int getChecksum()
     *          returns the checksum of the Packet
     *      int getPayload()
     *          returns the Packet's payload
     *
     */

    /*   Please use the following variables in your routines.
     *   int WindowSize  : the window size
     *   double RxmtInterval   : the retransmission timeout
     *   int LimitSeqNo  : when sequence number reaches this value, it wraps around
     */

    public static final int FirstSeqNo = 0;
    private int WindowSize;
    private double RxmtInterval;
    private int LimitSeqNo;
    // BELOW ARE MY ADDITIONS //
    private boolean Timerstarted = false;
    

    private ArrayList<Packet> WindowArray;
    private int Stored_ack;
    private ArrayList<Packet> PacketArray; // CREATED NEW ARRAYLIST FOR PACKETS //
    private int sequenceNumber;
    private Packet StoredPacket;  //for storing packets if window size at max //


    // B init variables //
    private Integer prevSeq;
    private Packet b_ackpacket;




    // Stat variables //
    private int OriginalTransmittedByA = 0;
    private int ReTransmissionByA = 0;
    private int DeliverLayer5B = 0;
    private int AckPacketsSentByB = 0;
    private int NumberOfCorruptedPackets = 0;
    private double Ratio_of_lostpackets = 0;
    private double Ratio_of_corrupted = 0;
    private int AVGRTT = 0;


    private double comSum = 0;
    private double comTot = 0;
    private double comStart = 0;
    private double comEnd = 0;


    private ArrayList<Boolean> RTT_table;
    private double RTT_sum = 0;
    private double RTT_trueNum = 0;
    


    
    // Add any necessary class variables here.  Remember, you cannot use
    // these variables to send messages error free!  They can only hold
    // state information for A or B.
    // Also add any necessary methods (e.g. checksum of a String)

    // This is the constructor.  Don't touch!
    public StudentNetworkSimulator(int numMessages,
                                   double loss,
                                   double corrupt,
                                   double avgDelay,
                                   int trace,
                                   int seed,
                                   int winsize,
                                   double delay)
    {
        super(numMessages, loss, corrupt, avgDelay, trace, seed);
	WindowSize = winsize;
	LimitSeqNo = winsize*2; // set appropriately
	RxmtInterval = delay;
    }

    
    // This routine will be called whenever the upper layer at the sender [A]
    // has a message to send.  It is the job of your protocol to insure that
    // the data in such a message is delivered in-order, and correctly, to
    // the receiving upper layer.
    protected void aOutput(Message message)
    {
        String msgData = message.getData();
        int charVal = 0;
        int stringVal = 0;
        for (int i = 0; i < msgData.length(); i++) {
            charVal = Character.getNumericValue((msgData.charAt(i)));
            stringVal = stringVal + charVal;
        }
        int checkSumA = stringVal + sequenceNumber + 0; 
        Packet p = new Packet(sequenceNumber, 0, checkSumA, msgData);
        sequenceNumber++;
        if (!Timerstarted) {
            startTimer(A, RxmtInterval);
            Timerstarted = true;
        }
        if ((WindowArray.size()) < WindowSize) {
            WindowArray.add(p);
            comStart = getTime();

            OriginalTransmittedByA++;
            RTT_table.add(true);
            RTT_trueNum++;
            System.out.println("Sent Packet from A to B");
            toLayer3(A, p);
        } else {
            System.out.println("Packet Window Full, packet added to buffer");
            PacketArray.add(p);
        }








       // System.out.println("Packet create TEST"); //
        


    }
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by a B-side procedure)
    // arrives at the A-side.  "packet" is the (possibly corrupted) packet
    // sent from the B-side.
    protected void aInput(Packet packet)
    {
        Integer checkSumChecker = 0;
        Integer b_acknum = packet.getAcknum();
        String checkSumString = ""; 

        checkSumString = packet.getPayload();
        for (int i = 0; i < checkSumString.length(); i++) {
            int charAint = checkSumString.charAt(i);
            checkSumChecker = checkSumChecker + charAint;

        }
        checkSumChecker += packet.getSeqnum() + b_acknum;

        if (checkSumChecker != (packet.getChecksum())) {  
            NumberOfCorruptedPackets++;
            System.out.println("Packet from B is corrupt");
            // says do nothing in psedocode??? //
        } else {
            if (b_acknum < Stored_ack) {
                NumberOfCorruptedPackets++;
                System.out.println("Packet from B is out of order!");
                // says do nothing in psedo code?
            } else {
                for (int i = 0; i < WindowArray.size(); i++) {
                    Packet loop_packet = WindowArray.get(i);
                    if ((loop_packet.getSeqnum()) <= b_acknum) {
                        WindowArray.remove(i);
                    }
                }
                Stored_ack = b_acknum;
                comEnd = getTime();   //for stats//
                comTot++;
                comSum += (comEnd - comStart);
                 //MAYBE DO RTT HERE???
                if (RTT_table.get(Stored_ack) == true) {
                    RTT_sum += (comEnd - comStart);
                }

                stopTimer(A);
                startTimer(A, RxmtInterval);
                
            }

        }
        PacketArray.sort(Comparator.comparing(Packet::getSeqnum));
        while ((WindowArray.size() < WindowSize) && (PacketArray.size() > 0)) {
            Packet Var = PacketArray.remove(0);
            RTT_table.add(true);
            RTT_trueNum++;
            WindowArray.add(Var);
            OriginalTransmittedByA++;
            System.out.println("Sent Packet from A to B");
            toLayer3(A, Var);
        }

        // Get packets from buffer, put into window, send toLayer3()




        
    }
    
    // This routine will be called when A's timer expires (thus generating a 
    // timer interrupt). You'll probably want to use this routine to control 
    // the retransmission of packets. See startTimer() and stopTimer(), above,
    // for how the timer is started and stopped. 
    protected void aTimerInterrupt()
    {   
 
        WindowArray.sort(Comparator.comparing(Packet::getSeqnum));
        startTimer(A, RxmtInterval); // rxtm sets how long the timer will expire
        for (int i = 0; i < WindowArray.size(); i++) {
            ReTransmissionByA++;
            RTT_table.add(false);
            System.out.println("Retransmission sent from A to B");
            toLayer3(A, WindowArray.get(i));
        }
    }
    
    // This routine will be called once, before any of your other A-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity A).
    protected void aInit()
    {
 
        WindowArray = new ArrayList<Packet>();
        PacketArray = new ArrayList<Packet>();
        sequenceNumber = FirstSeqNo;
        StoredPacket =  new Packet(0, 0, 0, "");
        Stored_ack = -1;



        RTT_table = new ArrayList<Boolean>();


        // do not declare here, set the values here // 
    }
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by an A-side procedure)
    // arrives at the B-side.  "packet" is the (possibly corrupted) packet
    // sent from the A-side.
    protected void bInput(Packet packet)  
    {
       // System.out.println(packet.getPayload());  // THIS IS TESTING CODE //
        String checkSumString = ""; 
        int checkSumChecker = 0;

        checkSumString = packet.getPayload();
        for (int i = 0; i < checkSumString.length(); i++) {
            int charBint = Character.getNumericValue((checkSumString.charAt(i)));
            checkSumChecker = checkSumChecker + charBint;
        }
        checkSumChecker += (packet.getSeqnum()) + (packet.getAcknum());
        if (checkSumChecker == (packet.getChecksum())) { 
            System.out.println("binput checksum okay");
            // this is when there is no corruption
            if ((packet.getSeqnum()) == (prevSeq + 1)) { // checking if it is in correct order, should be 1 more than the last one. For 0 I have the prevSeq at -1 so it should go to 0 and continue up

                prevSeq++;
                DeliverLayer5B++;
                toLayer5(packet.getPayload());
                b_ackpacket = new Packet(0, (packet.getSeqnum()), 0, "0000000000"); // this is a new packet that is blank in everything besides an ack, this is an immitation of irl. seqnum used as we dont normally have acknums except when b sends to a//
                
                checkSumChecker = 0;
                checkSumString = b_ackpacket.getPayload();
         
            for (int i = 0; i < checkSumString.length(); i++) {
                int charAint = (checkSumString.charAt(i));
                checkSumChecker = checkSumChecker + charAint;
            }
                checkSumChecker += b_ackpacket.getSeqnum() + b_ackpacket.getAcknum();
                b_ackpacket.setChecksum(checkSumChecker);
                AckPacketsSentByB++;
                System.out.println("Sent Packet from B to A");
                toLayer3(B, b_ackpacket);
                




            } else {         // my thoughts on this else is regardless of if the packet is out of order or dupe, both are discarded and we send prev ack. the dupe is the same are prev seq, but not seq +1 //
                b_ackpacket = new Packet(0, prevSeq, 0, "0000000000"); //Assuming ack is a number the same as seq, makes sense to reuse prevSeq for ack

                 checkSumChecker = 0;
                 checkSumString = b_ackpacket.getPayload();
         
            for (int i = 0; i < checkSumString.length(); i++) {
                int charAint = (checkSumString.charAt(i));
                checkSumChecker = checkSumChecker + charAint;
            }
                checkSumChecker += b_ackpacket.getSeqnum() + b_ackpacket.getAcknum();
                b_ackpacket.setChecksum(checkSumChecker);
                AckPacketsSentByB++;
                System.out.println("Sent Packet from B to A");
                toLayer3(B, b_ackpacket);

            }          
            


        } else {
            b_ackpacket = new Packet(0, prevSeq, 0, "0000000000");
            checkSumString = ""; 

            checkSumChecker = 0;
            checkSumString = b_ackpacket.getPayload();
         
            for (int i = 0; i < checkSumString.length(); i++) {
                int charAint = Character.getNumericValue((checkSumString.charAt(i)));
                checkSumChecker = checkSumChecker + charAint;
            }
                checkSumChecker += b_ackpacket.getSeqnum() + b_ackpacket.getAcknum();
                b_ackpacket.setChecksum(checkSumChecker);
            NumberOfCorruptedPackets++;
            AckPacketsSentByB++;
            System.out.println("Sent Packet from B to A");
            toLayer3(B, b_ackpacket);
             // this is when there is corruption. assuming I send the previous ack to a it will then start retransmitting
        }


    }
    
    // This routine will be called once, before any of your other B-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity B).
    protected void bInit()
    {
        prevSeq = -1;
        b_ackpacket = new Packet(0, 0, 0, "");

    }

    // Use to print final statistics
    protected void Simulation_done()
    {
    	// TO PRINT THE STATISTICS, FILL IN THE DETAILS BY PUTTING VARIABLE NAMES. DO NOT CHANGE THE FORMAT OF PRINTED OUTPUT
    	System.out.println("\n\n===============STATISTICS=======================");
    	System.out.println("Number of original packets transmitted by A: " + Integer.toString(OriginalTransmittedByA));
    	System.out.println("Number of retransmissions by A: " + Integer.toString(ReTransmissionByA));
    	System.out.println("Number of data packets delivered to layer 5 at B: " + Integer.toString(DeliverLayer5B));
    	System.out.println("Number of ACK packets sent by B: " + Integer.toString(AckPacketsSentByB));
    	System.out.println("Number of corrupted packets: " + Integer.toString(NumberOfCorruptedPackets));
    	System.out.println("Ratio of lost packets: " + Double.toString( 1.0 * (ReTransmissionByA - NumberOfCorruptedPackets) / (OriginalTransmittedByA + ReTransmissionByA)));
    	System.out.println("Ratio of corrupted packets: " + Double.toString(NumberOfCorruptedPackets * 1.0 / (OriginalTransmittedByA + ReTransmissionByA)));
    	System.out.println("Average RTT: " + Double.toString(RTT_sum / RTT_trueNum));
    	System.out.println("Average communication time: " + Double.toString((comSum / comTot)));
    	System.out.println("==================================================");

    	// PRINT YOUR OWN STATISTIC HERE TO CHECK THE CORRECTNESS OF YOUR PROGRAM
    	System.out.println("\nEXTRA:");
    	// EXAMPLE GIVEN BELOW
    	//System.out.println("Example statistic you want to check e.g. number of ACK packets received by A :" + "<YourVariableHere>"); 
    }	

}
