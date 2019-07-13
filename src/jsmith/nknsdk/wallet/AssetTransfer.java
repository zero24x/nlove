package jsmith.nknsdk.wallet;

import jsmith.nknsdk.client.NKNExplorer;

import java.math.BigDecimal;

/**
 *
 */
public class AssetTransfer {

    public final String toAddress;
    public final BigDecimal amount;

    public AssetTransfer(String toAddress, BigDecimal amount) {
        if (!NKNExplorer.isAddressValid(toAddress)) throw new IllegalArgumentException("Receiving address is not valid NKN address");
        this.toAddress = toAddress;
        this.amount = amount;
    }

}
