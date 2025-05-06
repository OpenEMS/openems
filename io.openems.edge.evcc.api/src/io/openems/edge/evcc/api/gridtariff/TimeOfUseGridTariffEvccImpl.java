package io.openems.edge.evcc.api.gridtariff;

import static io.openems.edge.timeofusetariff.api.utils.TimeOfUseTariffUtils.generateDebugLog;

import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.utils.ThreadPoolUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

@Designate(ocd = Config.class, factory = true)
@Component(//
        name = "TimeOfUseTariff.Grid.Evcc", //
        immediate = true, //
        configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class TimeOfUseGridTariffEvccImpl extends AbstractOpenemsComponent
        implements TimeOfUseTariff, OpenemsComponent, TimeOfUseGridTariffEvcc {

    //private final Logger log = LoggerFactory.getLogger(TimeOfUseGridTariffEvccImpl.class);
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final AtomicReference<TimeOfUsePrices> prices = new AtomicReference<>(TimeOfUsePrices.EMPTY_PRICES);
    private String apiUrl = "http://localhost:7070/api/tariff/grid";

    @Reference
    private ComponentManager componentManager;

    @Reference
    private Meta meta;

    private TimeOfUseGridTariffEvccApi apiClient;

    public TimeOfUseGridTariffEvccImpl() {
        super(OpenemsComponent.ChannelId.values(), TimeOfUseGridTariffEvcc.ChannelId.values());
    }

    @Activate
    private void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());

        if (!config.enabled()) {
            return;
        }

        this.apiUrl = config.apiUrl();
        this.apiClient = new TimeOfUseGridTariffEvccApi(this.apiUrl);
        this.executor.schedule(this.task, 0, TimeUnit.SECONDS);
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
        ThreadPoolUtils.shutdownAndAwaitTermination(this.executor, 0);
    }

    protected final Runnable task = () -> {
        this.prices.set(this.apiClient.fetchPrices());
    };

    public TimeOfUsePrices getPrices() {
        return TimeOfUsePrices.from(ZonedDateTime.now(), this.prices.get());
    }

    @Override
    public String debugLog() {
        return generateDebugLog(this, this.meta.getCurrency());
    }
}
