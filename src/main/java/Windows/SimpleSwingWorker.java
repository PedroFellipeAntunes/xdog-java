/**
 * Swing worker extension that doesn't swallow execptions
 * Code by JonathanGiles - https://github.com/JonathanGiles
 * https://www.jonathangiles.net/posts/2009/a-swingworker-that-doesnt-swallow-exceptions/
 */

package Windows;

import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

/**
 * Wrap the default SwingWorker for simple cases where 
 * we are NOT returned a result in such a way
 * that exceptions are not swallowed.
 */
public abstract class SimpleSwingWorker {

    private final SwingWorker worker = 
        new SwingWorker() {
        @Override
        protected Void doInBackground() throws Exception {
                SimpleSwingWorker.this.doInBackground();
                return null;
        }

        @Override
        protected void done() {
            // call get to make sure any exceptions 
            // thrown during doInBackground() are 
            // thrown again
            try {
                get();
            } catch (final InterruptedException ex) {
                throw new RuntimeException(ex);
            } catch (final ExecutionException ex) {
                throw new RuntimeException(ex.getCause());
            }
            SimpleSwingWorker.this.done();
        }
    };

    public SimpleSwingWorker() {}

    protected abstract Void doInBackground() throws Exception;

    protected abstract void done();

    public void execute() {
        worker.execute();
    }
}