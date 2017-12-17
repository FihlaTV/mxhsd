/*
 * mxhsd - Corporate Matrix Homeserver
 * Copyright (C) 2017 Maxime Dor
 *
 * https://www.kamax.io/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.kamax.mxhsd.api.event;

import com.google.gson.JsonObject;

public class NakedContentEvent extends NakedEvent {

    protected JsonObject content;

    protected NakedContentEvent() {
        // for subclasses only
    }

    public NakedContentEvent(JsonObject o) {
        super(o);
        this.content = EventKey.Content.getObj(o);
    }

    public NakedContentEvent(String type, String sender) {
        this(type, sender, new JsonObject());
    }

    public NakedContentEvent(String type, String sender, JsonObject content) {
        super(type, sender);
        this.content = content;
    }

    public JsonObject getContent() {
        return content;
    }

}
