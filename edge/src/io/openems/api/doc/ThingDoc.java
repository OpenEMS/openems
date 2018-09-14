/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016, 2017 FENECON GmbH and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *   FENECON GmbH - initial API and implementation and initial documentation
 *******************************************************************************/
package io.openems.api.doc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonObject;

import io.openems.api.thing.Thing;
import io.openems.core.utilities.InjectionUtils;

public class ThingDoc {

	private final Class<? extends Thing> clazz;
	private String title = "";
	private String text = "";
	private final Map<String, ChannelDoc> channels = new HashMap<>();
	private final Set<String> configChannels = new HashSet<>();

	public ThingDoc(Class<? extends Thing> clazz) {
		this.clazz = clazz;
	}

	public void setThingDescription(ThingInfo thing) {
		this.title = thing.title();
		this.text = thing.description();
	}

	public String getText() {
		return text;
	}

	public String getTitle() {
		return title;
	}

	public Class<? extends Thing> getClazz() {
		return clazz;
	}

	public void addChannelDoc(ChannelDoc doc) {
		this.channels.put(doc.getName().toLowerCase(), doc);
	}

	public void addConfigChannelDoc(ChannelDoc doc) {
		this.configChannels.add(doc.getName().toLowerCase());
	}

	public Optional<ChannelDoc> getChannelDoc(String channelId) {
		return Optional.ofNullable(this.channels.get(channelId.toLowerCase()));
	}

	public Collection<ChannelDoc> getChannelDocs() {
		return Collections.unmodifiableCollection(this.channels.values());
	}

	public Collection<ChannelDoc> getConfigChannelDocs() {
		Collection<ChannelDoc> result = new ArrayList<ChannelDoc>(this.configChannels.size());
		for (String configChannel : this.configChannels) {
			result.add(this.channels.get(configChannel));
		}
		return Collections.unmodifiableCollection(result);
	}

	public JsonObject getAsJsonObject() {
		JsonObject j = new JsonObject();
		j.addProperty("class", getClazz() != null ? getClazz().getName() : "");
		j.addProperty("title", getTitle());
		j.addProperty("text", getText());
		JsonObject jChannels = new JsonObject();
		for (String configChannel : this.configChannels) {
			ChannelDoc channelDoc = this.channels.get(configChannel);
			jChannels.add(channelDoc.getName(), channelDoc.getAsJsonObject());
		}
		j.add("channels", jChannels);
		j.add("implements", InjectionUtils.getImplementsAsJson(getClazz()));
		return j;
	}
}
