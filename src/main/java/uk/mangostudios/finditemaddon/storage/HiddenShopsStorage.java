package uk.mangostudios.finditemaddon.storage;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import uk.mangostudios.finditemaddon.FindItemAddOn;
import uk.mangostudios.finditemaddon.storage.impl.FinePosition;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class HiddenShopsStorage {

    private static HiddenShopsStorage instance;
    private final FindItemAddOn plugin;

    public HiddenShopsStorage(FindItemAddOn plugin) {
        this.plugin = plugin;
        instance = this;
    }

    /**
     * Save all users to the JSON file
     */
    public CompletableFuture<Void> saveAll(Map<UUID, List<FinePosition>> hiddenUsers) {
        return CompletableFuture.runAsync(() -> {
            try {
                final Gson gson = new GsonBuilder().setPrettyPrinting().create();
                final File file = new File(plugin.getDataFolder().getAbsolutePath() + "/data/userdata.json");

                if (!file.exists()) {
                    file.getParentFile().mkdir(); // Creates the /data/
                    file.createNewFile(); // Creates the /data/userdata.json
                }

                if (hiddenUsers != null) {
                    final Writer writer = new FileWriter(file, false);
                    gson.toJson(hiddenUsers, writer);
                    writer.flush();
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Load all hidden users from the JSON file
     */
    public CompletableFuture<Map<UUID, List<FinePosition>>> load() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final Gson gson = new Gson();
                final File file = new File(plugin.getDataFolder().getAbsolutePath() + "/data/userdata.json");

                if (!file.exists()) {
                    file.getParentFile().mkdir(); // Creates the /data/
                    file.createNewFile(); // Creates the /data/userdata.json
                }

                if (file.exists()) {
                    final Reader reader = new FileReader(file);
                    final TypeToken<Map<UUID, List<FinePosition>>> typeToken = new TypeToken<>() {
                    };
                    final Map<UUID, List<FinePosition>> hiddenUsers = gson.fromJson(reader, typeToken.getType());
                    if (hiddenUsers != null) {
                        return hiddenUsers;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    public static HiddenShopsStorage get() {
        return instance;
    }
}
