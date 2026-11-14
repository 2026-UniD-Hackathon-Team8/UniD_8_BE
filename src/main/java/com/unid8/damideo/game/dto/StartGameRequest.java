package com.unid8.damideo.game.dto;

import com.unid8.damideo.game.GameMode;

public record StartGameRequest(
        GameMode gameMode
) {

    public GameMode resolvedMode() {
        return gameMode == null ? GameMode.NORMAL : gameMode;
    }
}
