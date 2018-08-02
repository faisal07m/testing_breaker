package de.upb.bibifi2018.kaffeeklatsch.bank;

import com.codahale.xsalsa20poly1305.SimpleBox;
import de.upb.bibifi2018.kaffeeklatsch.Printer;
import de.upb.bibifi2018.kaffeeklatsch.protocol.CryptoConnection;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.ProtocolFailureException;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.ReplayAttackException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import okio.ByteString;

/**
 * This class implements the Listening for incoming TCP connections
 * on the port set by the user.
 * <br><br>
 *
 * The {@link #run()} method will most likely run on the main application thread,
 * while accepted connections will be dispatched using a limited {@link ThreadPoolExecutor}.
 * If the thread pool is full, it will block the main thread, since they communicate using a
 * {@link SynchronousQueue}, which will make the {@link ServerSocket} queue up connections until
 * its backlog (50 per default) is full, after that it will reject further connections.
 * <br><br>
 *
 * A dispatched client connection on the executor will be monitored using a {@link Future},
 * and a {@link ScheduledThreadPoolExecutor} that fires 10 seconds after the connection was accepted
 * and interrupts its processing to force a timeout.
 * <br><br>
 *
 *
 */
public class Server implements Runnable {
  private final ThreadPoolExecutor threadPoolExecutor;
  private final ScheduledThreadPoolExecutor timeoutScheduler;
  private final BankReaktor bankReaktor;
  private final ServerSocket serverSocket;
  private final ByteString bankPrivateKey;
  private final SimpleBox simpleBox;

  public Server(int port, ByteString atmPublicKey, ByteString bankPrivateKey) throws IOException {
    serverSocket = new ServerSocket(port);
    this.bankPrivateKey = bankPrivateKey;

    this.threadPoolExecutor = new ThreadPoolExecutor(
        1, 64, 10, TimeUnit.SECONDS, new SynchronousQueue<>()
    );

    this.timeoutScheduler = new ScheduledThreadPoolExecutor(1);

    this.bankReaktor = new BankReaktor();
    this.simpleBox = new SimpleBox(atmPublicKey, bankPrivateKey);
  }


  private void handleAccept(Socket accept) {


    // need a holder object to inject a reference into the lambda after creating it.
    CallableHolder holder = new CallableHolder();

    Future<?> submit = this.threadPoolExecutor.submit(() -> handleClient(accept, holder));

    ScheduledFuture<Boolean> timeoutFuture = this.timeoutScheduler.schedule(
        () -> submit.cancel(true),
        10, TimeUnit.SECONDS
    );

    // clear timeout job on success, to avoid leaving stale jobs in the scheduled executor
    holder.setRunnable(() -> timeoutFuture.cancel(false));
  }


  private void handleClient(Socket accept, CallableHolder holder) {
    try {
      BankProtocolServer bankProtocolServer = new BankProtocolServer(
          simpleBox, bankPrivateKey, bankReaktor
      );

      CryptoConnection connection = new CryptoConnection(accept, simpleBox);
      connection.sendMessage(bankProtocolServer.sendChallenge());

      // Receive Command
      ByteString incomingMessage = connection.readMessage();

      ByteString result = bankProtocolServer.processRequest(incomingMessage);

      // Send Response
      connection.sendMessage(result);
      connection.close();

    } catch (ProtocolFailureException | ReplayAttackException | IOException e) {
      Printer.printProtocolError();
    } finally {
      try {
        accept.close();
      } catch (IOException e) {
        System.err.println("error closing atm socket after transaction");
      }
      holder.execute();
    }
  }

  @Override
  public void run() {
    while (true) {
      try {
        Socket accept = this.serverSocket.accept();
        handleAccept(accept);
      } catch (IOException e) {
        Printer.printProtocolError();
      }
    }
  }

  private static class CallableHolder {
    volatile Runnable runnable;

    public void execute() {
      if (runnable != null) {
        runnable.run();
      }
    }

    public void setRunnable(Runnable runnable) {
      this.runnable = runnable;
    }
  }
}
