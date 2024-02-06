	private ImmutableTable<String, String, JsonElement> collectData(List<OpenemsComponent> enabledComponents) {
		try {
			return enabledComponents.parallelStream() //
					.flatMap(component -> component.channels().parallelStream()) //
					.filter(channel -> // Ignore WRITE_ONLY Channels
					channel.channelDoc().getAccessMode() != AccessMode.WRITE_ONLY //
							// Ignore Low-Priority Channels
							&& channel.channelDoc().getPersistencePriority()
									.isAtLeast(this.parent.config.persistencePriority()))
					.collect(ImmutableTable.toImmutableTable(c -> c.address().getComponentId(),
							c -> c.address().getChannelId(), c -> c.value().asJson()));
			// TODO remove values for disappeared components
//			final Set<String> enabledComponentIds = enabledComponents.stream() //
//					.map(c -> c.id()) //
//					.collect(Collectors.toSet());
//			this.lastValues.rowMap().entrySet().stream() //
//					.filter(row -> !enabledComponentIds.contains(row.getKey())) //
//					.forEach(row -> {
//						row.getValue().entrySet().parallelStream() //
//								.forEach(column -> {
//									this.publish(row.getKey() + "/" + column.getKey(), JsonNull.INSTANCE.toString());
//								});
//					});
		} catch (Exception e) {
			// ConcurrentModificationException can happen if Channels are dynamically added
			// or removed
			return ImmutableTable.of();
		}
	}
