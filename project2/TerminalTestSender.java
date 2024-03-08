package org.example.project2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class TerminalTestSender {
    public static void main(String[] args) throws IOException {

        SenderTCP senderTCP = new SenderTCP(); 
        SenderUDP senderUDP = new SenderUDP();

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String in;  
        int mode;

        /* Choose protocol */
        System.out.println("0: TCP\t1:RBUDP");
        System.out.print("Select protocol as listed above >> ");
        /////mode = Integer.parseInt(reader.readLine());
        mode = 1;
        

        switch(mode) {
            case 0: // TCP

                /* Get IP address */
                System.out.print("Enter IP >> ");
                in = reader.readLine(); 
                senderTCP.setIP(in);

                /* Get port number */
                System.out.print("Enter Port for TCP >> ");
                in = reader.readLine();
                senderTCP.setPort(Integer.parseInt(in));

                System.out.println("Enter file location to send >> ");
                in = reader.readLine();
                if (!senderTCP.setFileDest(in)) { System.out.println("File not found."); return; }

                senderTCP.initSocket();
                senderTCP.initObjectSender();
                senderTCP.send();

                break;

            case 1: // RBUDP
                /* Get IP address */
                System.out.print("Enter IP >> ");
                /////in = reader.readLine();
                in = "localhost";
                senderUDP.setInetAddress(in);

                /* Get port number */
                System.out.print("Enter Port for TCP >> ");
                /////in = reader.readLine();
                in = "4166";
                senderUDP.setPort(Integer.parseInt(in));

                /* Select file to send */
                System.out.print("Enter file location to send >> ");
                /////in = reader.readLine();
                in = "/Users/joshuajamesventer/Git/group_18_2/files_to_send/fuck.txt";
                if (!senderUDP.setFileDest(in)) { System.out.println("File not found."); return; }

                System.out.print("Enter packet size(Bytes) >>");
                /////in = reader.readLine();
                in = "1024";
                senderUDP.setPacketSize(Integer.parseInt(in));

                System.out.print("Enter blast length (1-100) >>");
                /////in = reader.readLine();
                in = "20";
                senderUDP.setBlastLength(Integer.parseInt(in));

                senderUDP.initSocket();
                senderUDP.send();
                
                break;
        }     
    }
}
