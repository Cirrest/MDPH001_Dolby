package com.mdph.dolbycontrol;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;

final class DolbyDsBackend implements AutoCloseable {
    interface Listener {
        void onConnected();

        void onDisconnected();
    }

    static final class ProfileSettings implements Parcelable {
        static final Parcelable.Creator<ProfileSettings> CREATOR =
                new Parcelable.Creator<ProfileSettings>() {
                    @Override
                    public ProfileSettings createFromParcel(Parcel source) {
                        return new ProfileSettings(source);
                    }

                    @Override
                    public ProfileSettings[] newArray(int size) {
                        return new ProfileSettings[size];
                    }
                };

        boolean geqEnabled;
        boolean dialogEnabled;
        boolean volumeLeveler;
        boolean headphoneVirtualizer;
        boolean speakerVirtualizer;

        ProfileSettings() {
        }

        private ProfileSettings(Parcel source) {
            boolean[] values = new boolean[5];
            source.readBooleanArray(values);
            geqEnabled = values[0];
            dialogEnabled = values[1];
            volumeLeveler = values[2];
            headphoneVirtualizer = values[3];
            speakerVirtualizer = values[4];
        }

        ProfileSettings copy() {
            ProfileSettings copy = new ProfileSettings();
            copy.geqEnabled = geqEnabled;
            copy.dialogEnabled = dialogEnabled;
            copy.volumeLeveler = volumeLeveler;
            copy.headphoneVirtualizer = headphoneVirtualizer;
            copy.speakerVirtualizer = speakerVirtualizer;
            return copy;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel destination, int flags) {
            destination.writeBooleanArray(new boolean[] {
                    geqEnabled,
                    dialogEnabled,
                    volumeLeveler,
                    headphoneVirtualizer,
                    speakerVirtualizer
            });
        }
    }

    private interface RequestWriter {
        void write(Parcel data);
    }

    private interface ReplyReader<T> {
        T read(Parcel reply);
    }

    private final Context context;
    private final Listener listener;
    private final int clientHandle;
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = service;
            listener.onConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            binder = null;
            listener.onDisconnected();
        }

        @Override
        public void onBindingDied(ComponentName name) {
            binder = null;
            bound = false;
            listener.onDisconnected();
            bind();
        }

        @Override
        public void onNullBinding(ComponentName name) {
            binder = null;
            listener.onDisconnected();
        }
    };

    private volatile IBinder binder;
    private boolean bound;

    DolbyDsBackend(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        clientHandle = System.identityHashCode(this);
    }

    boolean bind() {
        if (bound) {
            return true;
        }
        Intent intent = new Intent(DolbyDsProtocol.INTERFACE_TOKEN);
        intent.setComponent(new ComponentName(
                DolbyDsProtocol.BACKEND_PACKAGE,
                DolbyDsProtocol.BACKEND_SERVICE));
        bound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
        return bound;
    }

    boolean isConnected() {
        IBinder current = binder;
        return current != null && current.isBinderAlive();
    }

    boolean getEnabled() {
        return transact(
                DolbyDsProtocol.TRANSACTION_GET_DS_ON,
                new RequestWriter() {
                    @Override
                    public void write(Parcel data) {
                        data.writeInt(1);
                    }
                },
                new ReplyReader<Boolean>() {
                    @Override
                    public Boolean read(Parcel reply) {
                        checkStatus("getDsOn", reply.readInt());
                        boolean[] enabled = new boolean[1];
                        reply.readBooleanArray(enabled);
                        return enabled[0];
                    }
                });
    }

    void setEnabled(final boolean enabled) {
        transactStatus(
                DolbyDsProtocol.TRANSACTION_SET_DS_ON,
                "setDsOn",
                new RequestWriter() {
                    @Override
                    public void write(Parcel data) {
                        data.writeInt(clientHandle);
                        data.writeInt(enabled ? 1 : 0);
                    }
                });
    }

    int getProfileCount() {
        return readSingleInt(
                DolbyDsProtocol.TRANSACTION_GET_PROFILE_COUNT,
                "getProfileCount");
    }

    int getProfile() {
        return readSingleInt(
                DolbyDsProtocol.TRANSACTION_GET_SELECTED_PROFILE,
                "getSelectedProfile");
    }

    void setProfile(final int profile) {
        transactStatus(
                DolbyDsProtocol.TRANSACTION_SET_SELECTED_PROFILE,
                "setSelectedProfile",
                new RequestWriter() {
                    @Override
                    public void write(Parcel data) {
                        data.writeInt(clientHandle);
                        data.writeInt(profile);
                    }
                });
    }

    ProfileSettings getProfileSettings(final int profile) {
        return transact(
                DolbyDsProtocol.TRANSACTION_GET_PROFILE_SETTINGS,
                new RequestWriter() {
                    @Override
                    public void write(Parcel data) {
                        data.writeInt(profile);
                        data.writeInt(1);
                    }
                },
                new ReplyReader<ProfileSettings>() {
                    @Override
                    public ProfileSettings read(Parcel reply) {
                        checkStatus("getProfileSettings", reply.readInt());
                        ProfileSettings[] settings =
                                reply.createTypedArray(ProfileSettings.CREATOR);
                        if (settings == null || settings.length == 0 || settings[0] == null) {
                            throw new IllegalStateException("getProfileSettings returned no data");
                        }
                        return settings[0];
                    }
                });
    }

    void setProfileSettings(final int profile, final ProfileSettings settings) {
        transactStatus(
                DolbyDsProtocol.TRANSACTION_SET_PROFILE_SETTINGS,
                "setProfileSettings",
                new RequestWriter() {
                    @Override
                    public void write(Parcel data) {
                        data.writeInt(clientHandle);
                        data.writeInt(profile);
                        data.writeInt(1);
                        settings.writeToParcel(data, 0);
                    }
                });
    }

    int getIeqPreset(final int profile) {
        return readSingleInt(
                DolbyDsProtocol.TRANSACTION_GET_IEQ_PRESET,
                "getIeqPreset",
                new RequestWriter() {
                    @Override
                    public void write(Parcel data) {
                        data.writeInt(profile);
                    }
                });
    }

    void setIeqPreset(final int profile, final int preset) {
        transactStatus(
                DolbyDsProtocol.TRANSACTION_SET_IEQ_PRESET,
                "setIeqPreset",
                new RequestWriter() {
                    @Override
                    public void write(Parcel data) {
                        data.writeInt(clientHandle);
                        data.writeInt(profile);
                        data.writeInt(preset);
                    }
                });
    }

    float[] getGeq(final int profile, final int preset, final int bandCount) {
        return transact(
                DolbyDsProtocol.TRANSACTION_GET_GEQ,
                new RequestWriter() {
                    @Override
                    public void write(Parcel data) {
                        data.writeInt(profile);
                        data.writeInt(preset);
                        data.writeInt(bandCount);
                    }
                },
                new ReplyReader<float[]>() {
                    @Override
                    public float[] read(Parcel reply) {
                        checkStatus("getGeq", reply.readInt());
                        float[] values = new float[bandCount];
                        reply.readFloatArray(values);
                        return values;
                    }
                });
    }

    void setGeq(final int profile, final int preset, final float[] gains) {
        transactStatus(
                DolbyDsProtocol.TRANSACTION_SET_GEQ,
                "setGeq",
                new RequestWriter() {
                    @Override
                    public void write(Parcel data) {
                        data.writeInt(clientHandle);
                        data.writeInt(profile);
                        data.writeInt(preset);
                        data.writeFloatArray(gains);
                    }
                });
    }

    void setDsApParam(final String parameter, final int value) {
        transactStatus(
                DolbyDsProtocol.TRANSACTION_SET_DS_AP_PARAM,
                "setDsApParam " + parameter,
                new RequestWriter() {
                    @Override
                    public void write(Parcel data) {
                        data.writeInt(clientHandle);
                        data.writeString(parameter);
                        data.writeIntArray(new int[] {value});
                    }
                });
    }

    @Override
    public void close() {
        binder = null;
        if (bound) {
            context.unbindService(connection);
            bound = false;
        }
    }

    private int readSingleInt(int transaction, String operation) {
        return readSingleInt(transaction, operation, new RequestWriter() {
            @Override
            public void write(Parcel data) {
            }
        });
    }

    private int readSingleInt(
            int transaction, final String operation, RequestWriter writer) {
        return transact(
                transaction,
                new ArrayRequestWriter(writer),
                new ReplyReader<Integer>() {
                    @Override
                    public Integer read(Parcel reply) {
                        checkStatus(operation, reply.readInt());
                        int[] value = new int[1];
                        reply.readIntArray(value);
                        return value[0];
                    }
                });
    }

    private void transactStatus(int transaction, final String operation, RequestWriter writer) {
        transact(
                transaction,
                writer,
                new ReplyReader<Void>() {
                    @Override
                    public Void read(Parcel reply) {
                        checkStatus(operation, reply.readInt());
                        return null;
                    }
                });
    }

    private <T> T transact(int transaction, RequestWriter writer, ReplyReader<T> reader) {
        IBinder current = binder;
        if (current == null || !current.isBinderAlive()) {
            throw new IllegalStateException("Dolby DsService is not connected");
        }
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(DolbyDsProtocol.INTERFACE_TOKEN);
            writer.write(data);
            if (!current.transact(transaction, data, reply, 0)) {
                throw new IllegalStateException(
                        "Dolby DsService rejected transaction " + transaction);
            }
            reply.readException();
            return reader.read(reply);
        } catch (RemoteException error) {
            throw new IllegalStateException("Dolby DsService transaction failed", error);
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    private static void checkStatus(String operation, int status) {
        if (status != 0) {
            throw new IllegalStateException(operation + " returned " + status);
        }
    }

    private static final class ArrayRequestWriter implements RequestWriter {
        private final RequestWriter delegate;

        ArrayRequestWriter(RequestWriter delegate) {
            this.delegate = delegate;
        }

        @Override
        public void write(Parcel data) {
            delegate.write(data);
            data.writeInt(1);
        }
    }
}
