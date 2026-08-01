package com.mdph.dolbycontrol;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class SessionControllerRegistry<T extends AutoCloseable> implements AutoCloseable {
    interface Factory<T> {
        T open(int sessionId) throws Exception;
    }

    interface Operation<T> {
        void run(T controller) throws Exception;
    }

    private final Factory<T> factory;
    private final Map<Integer, T> controllers = new LinkedHashMap<Integer, T>();

    SessionControllerRegistry(Factory<T> factory) {
        this.factory = factory;
    }

    void synchronize(Set<Integer> activeSessionIds, Operation<T> initializer)
            throws Exception {
        Iterator<Map.Entry<Integer, T>> iterator = controllers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, T> entry = iterator.next();
            if (!activeSessionIds.contains(entry.getKey())) {
                entry.getValue().close();
                iterator.remove();
            }
        }

        for (Integer sessionId : activeSessionIds) {
            if (controllers.containsKey(sessionId)) {
                continue;
            }
            T controller = factory.open(sessionId);
            try {
                initializer.run(controller);
                controllers.put(sessionId, controller);
            } catch (Exception error) {
                try {
                    controller.close();
                } catch (Exception closeError) {
                    error.addSuppressed(closeError);
                }
                throw error;
            }
        }
    }

    void apply(Operation<T> operation) throws Exception {
        for (T controller : controllers.values()) {
            operation.run(controller);
        }
    }

    T get(int sessionId) {
        return controllers.get(sessionId);
    }

    int size() {
        return controllers.size();
    }

    @Override
    public void close() throws Exception {
        Exception firstError = null;
        for (T controller : controllers.values()) {
            try {
                controller.close();
            } catch (Exception error) {
                if (firstError == null) {
                    firstError = error;
                } else {
                    firstError.addSuppressed(error);
                }
            }
        }
        controllers.clear();
        if (firstError != null) {
            throw firstError;
        }
    }
}
