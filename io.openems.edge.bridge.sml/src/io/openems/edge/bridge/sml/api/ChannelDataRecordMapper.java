package io.openems.edge.bridge.sml.api;

import java.util.Arrays;
import java.util.List;
import org.openmuc.jsml.structures.EMessageBody;
import org.openmuc.jsml.structures.SmlFile;
import org.openmuc.jsml.structures.SmlList;
import org.openmuc.jsml.structures.SmlListEntry;
import org.openmuc.jsml.structures.SmlMessage;
import org.openmuc.jsml.structures.responses.SmlGetListRes;

import io.openems.edge.common.channel.Channel;

public class ChannelDataRecordMapper {
	protected SmlFile data;

	protected List<ChannelRecord> channelDataRecordsList;

	public ChannelDataRecordMapper(SmlFile data, List<ChannelRecord> channelDataRecordsList) {
		this.data = data;
		this.channelDataRecordsList = channelDataRecordsList;

		for (ChannelRecord channelRecord : channelDataRecordsList) {
			this.mapDataToChannel(data, channelRecord.getChannel(), channelRecord.getObisCode());
		}
	}

	public SmlFile getData() {
		return this.data;
	}

	public void setData(SmlFile data) {
		this.data = data;
	}

	public List<ChannelRecord> getChannelDataRecordsList() {
		return this.channelDataRecordsList;
	}

	public void setChannelDataRecordsList(List<ChannelRecord> channelDataRecordsList) {
		this.channelDataRecordsList = channelDataRecordsList;
	}

	protected void mapDataToChannel(SmlFile smlFile, Channel<?> channel, byte[] obisCode) {
		// Extract messages from SML file
		List<SmlMessage> smlMessages = data.getMessages();

		if (smlMessages != null) {
			int messageCount = smlMessages.size();

			if (messageCount <= 0) {
				// logger.warn("{}: no valid SML messages list retrieved.", this.toString());
			}

			for (int messageIndex = 0; messageIndex < messageCount; messageIndex++) {
				SmlMessage smlMessage = data.getMessages().get(messageIndex);

				int tag = smlMessage.getMessageBody().getTag().id();
				if (tag != EMessageBody.GET_LIST_RESPONSE.id()) {
					// Skip non data messages of the SML file
					continue;
				}

				// Get value list
				SmlGetListRes listResponse = data.getMessages().get(messageIndex).getMessageBody().getChoice();
				SmlList smlValueList = listResponse.getValList();
				SmlListEntry[] smlListEntries = smlValueList.getValListEntry();

				for (var entry : smlListEntries) {
					var valueConverter = new SmlListEntryValueConverter(entry);

					if (Arrays.equals(valueConverter.getObisCode(), obisCode)) {
						if (valueConverter.getDataType().contains("String")) {
							channel.setNextValue(valueConverter.getConvertedString());
							return;
						} else if (valueConverter.getDataType().contains("Boolean")) {
							channel.setNextValue(valueConverter.getConvertedBoolean());
							return;
						} else if (valueConverter.getDataType().contains("Integer")
								|| valueConverter.getDataType().contains("Unsigned")) {
							channel.setNextValue(valueConverter.getConvertedDouble());
							return;
						}
					}
				}
			}
		}
	}
}
