
package com.github.kaiwinter.nfcsonos.rest.favorite.model;

import java.util.List;

public class Item {

    public String id;
    public String name;
    public String description;
    public String imageUrl;
    public List<Image> images = null;
    public Service service;

    @Override
    public String toString() {
        return id + ": " + name;
    }
}
