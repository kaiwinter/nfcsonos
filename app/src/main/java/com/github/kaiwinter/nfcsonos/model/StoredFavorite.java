package com.github.kaiwinter.nfcsonos.model;

import com.github.kaiwinter.nfcsonos.rest.model.Item;

import java.io.Serializable;

/**
 * A favorite which is persisted on the device. It is used to provide faster user feedback when a NFC tag was scanned.
 */
public class StoredFavorite implements Serializable {
    public String id;
    public String name;
    public String description;
    public String imageUrl;

    /**
     * Creates a {@link StoredFavorite} from a {@link Item}
     *
     * @param item the origin {@link Item}
     * @return a {@link StoredFavorite}
     */
    public static StoredFavorite fromItem(Item item) {
        StoredFavorite storedFavorite = new StoredFavorite();
        storedFavorite.id = item.id;
        storedFavorite.name = item.name;
        storedFavorite.description = item.description;
        storedFavorite.imageUrl = item.imageUrl;
        return storedFavorite;
    }
}