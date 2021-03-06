package com.thinkerwolf.gamer.grizzly;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.logging.Logger;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.utils.StringFilter;

/**
 * The simple client, which sends a message to the echo server and waits for
 * response
 */
public class EchoClient {
    private static final Logger logger = Grizzly.logger(EchoClient.class);

    static Connection[] connections;

    public static void main(String[] args)
            throws IOException, ExecutionException, InterruptedException, TimeoutException {
        try {
            startConnection(10);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            System.out.println("Ready... (\"q\" to exit)");
            final BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in));
            do {
                final String userInput = inReader.readLine();
                if (userInput == null || "q".equals(userInput)) {
                    break;
                }
                int idx=  0;
                for (Connection connection : connections) {
                    connection.write(userInput + "-" + idx++);
                }

            } while (true);
        } finally {

        }
    }

    private static void startConnection(int num) throws Exception {
        connections = new Connection[num];
        for (int i = 0; i < num; i++) {
            Connection connection = null;

            // Create a FilterChain using FilterChainBuilder
            FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
            // Add TransportFilter, which is responsible
            // for reading and writing data to the connection
            filterChainBuilder.add(new TransportFilter());
            // StringFilter is responsible for Buffer <-> String conversion
            filterChainBuilder.add(new StringFilter(Charset.forName("UTF-8")));
            // ClientFilter is responsible for redirecting server responses to the
            // standard output
            filterChainBuilder.add(new ClientFilter());

            // Create TCP transport
            final TCPNIOTransport transport = TCPNIOTransportBuilder.newInstance().build();
            transport.setProcessor(filterChainBuilder.build());


            // start the transport
            transport.start();
            // perform async. connect to the server
            Future<Connection> future = transport.connect(EchoServer.HOST, EchoServer.PORT);
            connections[i] = future.get(10, TimeUnit.SECONDS);

        }

    }


}
