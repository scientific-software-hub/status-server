package wpn.hdri.ss.storage;

import java.util.concurrent.*;

/**
 * Decorates storage in a way that all the method are executed in a single thread sequentially
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 13.06.13
 */
public class SingleThreadStorage implements Storage{
    private final ExecutorService exec = Executors.newSingleThreadExecutor();

    private final Storage decorated;

    public SingleThreadStorage(Storage decorated) {
        this.decorated = decorated;
    }


    @Override
    public void save(final String dataName, final Iterable<String> header, final Iterable<Iterable<String>> body) throws StorageException {
        exec.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                decorated.save(dataName,header,body);
                return null;
            }
        });
    }

    @Override
    public <T> Iterable<T> load(String dataName, TypeFactory<T> factory) throws StorageException {
        Future<Iterable<T>> future = exec.submit(new Callable<Iterable<T>>() {
            @Override
            public Iterable<T> call() throws Exception {
                throw new UnsupportedOperationException("This method is not supported in " + this.getClass());
            }
        });

        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StorageException(e);
        } catch (ExecutionException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void delete(final String dataName) throws StorageException {
        exec.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                decorated.delete(dataName);
                return null;
            }
        });
    }
}
