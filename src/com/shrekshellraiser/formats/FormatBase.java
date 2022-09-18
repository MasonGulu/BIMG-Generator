package com.shrekshellraiser.formats;

import com.shrekshellraiser.blit.BlitMap;

import java.io.File;

public abstract class FormatBase {
    public abstract void save(BlitMap[] blitMaps, File file);

    public abstract String getName();

    public boolean supportCharacters() {
        return true;
    }

    public boolean supportCustomPalette() {
        return true;
    }
}
