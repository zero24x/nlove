package com.nlove.iota.util;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.iota.jota.IotaAPI;
import org.iota.jota.account.AccountOptions;
import org.iota.jota.account.AccountStateManager;
import org.iota.jota.account.event.EventManager;
import org.iota.jota.account.event.events.EventReattachment;
import org.iota.jota.dto.response.ReplayBundleResponse;
import org.iota.jota.model.Bundle;
import org.iota.jota.model.Transaction;
import org.iota.jota.types.Hash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PromoteReattachUtil {

    private static final Logger log = LoggerFactory.getLogger(PromoteReattachUtil.class);

    private static final int APROX_ABOVE_MAX_DEPTH_MIN = 5;

    private EventManager eventManager;

    private IotaAPI api;

    private AccountStateManager manager;

    private AccountOptions options;

    /**
     * Original tail mapped to its original tail tx and its reattachment tx
     */
    private Map<String, List<Transaction>> bundleTails;

    public PromoteReattachUtil(EventManager eventManager, IotaAPI api, AccountStateManager manager, AccountOptions options) {
        this.eventManager = eventManager;
        this.api = api;
        this.manager = manager;
        this.options = options;
    }

    public void doTask(Bundle bundle) {
        try {

            String promotableTail = bundle.getTransactions().get(0).getHash();
            if (promotableTail != null) {
                promote(bundle, promotableTail);
            } else {
                reattach(bundle);
            }
        } catch (Exception e) {
            log.error("Failed to run promote task for " + bundle.getBundleHash() + ": " + e.getMessage());
        }
    }

    private void promote(Bundle pendingBundle) {
        this.promote(pendingBundle, pendingBundle.getTransactions().get(0).getHash());
    }

    public void promote(Bundle pendingBundle, String promotableTail) {
        List<Transaction> res = api.promoteTransaction(promotableTail, options.getDepth(), options.getMwm(), pendingBundle);
    }

    public void reattach(Bundle pendingBundle) {
        Bundle newBundle = createReattachBundle(pendingBundle);
        Collections.reverse(newBundle.getTransactions());

        manager.addTailHash(new Hash(pendingBundle.getTransactions().get(0).getHash()), new Hash(newBundle.getTransactions().get(0).getHash()));

        EventReattachment event = new EventReattachment(pendingBundle, newBundle);
        eventManager.emit(event);

        promote(newBundle);
    }

    private Bundle createReattachBundle(Bundle pendingBundle) {
        ReplayBundleResponse ret = api.replayBundle(pendingBundle, options.getDepth(), options.getMwm(), null);

        return ret.getNewBundle();
    }

}
