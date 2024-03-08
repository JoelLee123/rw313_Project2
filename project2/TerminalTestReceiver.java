package org.example.project2;

import java.io.IOException;

public class TerminalTestReceiver {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        
        /* OUTLINE OF PROGRAM
         * 1. Input TCP and RBUDP ports.
         * 2. Select UDP socket timeout.
         * 3. Select out directory.
         * 4. Select Ready. 
         */
        String in; 

        /* Construct TCP & RBUDP Receivers */
        ReceiverTCP receiverTCP = new ReceiverTCP();
        ReceiverUDP receiverUDP = new ReceiverUDP(); 

        System.out.println("Receiver.java running...");

        /* Get input of TCP and RBUDP ports */
        System.out.print("Enter TCP Port >> ");
        /////in = reader.readLine();
        in = "4165";
        receiverTCP.setPort(Integer.parseInt(in));
        receiverUDP.setPort(Integer.parseInt(in)+1);

        /* Select out directory (for both protocols) */
        System.out.print("Enter destination dir >> ");
        
        //////in = reader.readLine();
        in = "./destination/";
        receiverTCP.setDir(in);
        receiverUDP.setDir(in);

        /* Set UDP socket timeout */
        System.out.print("Enter UDP Socket timeout(ms) >>");
        /////in = reader.readLine();
        in = "500";
       /*receiverTCP.initServerSocket();
        receiverTCP.initObjectReceiver();*/
        receiverUDP.initSocket();
        receiverUDP.setTimeout(Integer.parseInt(in));
        receiverUDP.buildMetaData();

        // receiverUDP.initSocket();
        // receiverUDP.buildMetaData();
    /*
        new Thread() {
            public void run() {
                if (!receiverTCP.initServerSocket()) System.out.println("Initialise Server Socket Failed");
                if (!receiverTCP.initObjectReceiver()) System.out.println("Initialise Object Senders Failed");
                receiverTCP.accept(); 
            }
        }.start();
        
        
        // TODO SANGO: initialise sockets in here since ServerSocket.accecpt() is a blocking method.
        /*
        new Thread() {
            public void run() {
                receiverUDP.initSocket();
                receiverUDP.buildMetaData();
            }
        }.start();
        */

    }
}
