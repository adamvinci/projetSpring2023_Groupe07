package be.vinci.ipl.vsx;

import be.vinci.ipl.vsx.models.FilledQuantity;
import be.vinci.ipl.vsx.models.Position;
import be.vinci.ipl.vsx.models.Transaction;
import be.vinci.ipl.vsx.repositories.OrderProxy;
import be.vinci.ipl.vsx.repositories.PriceProxy;
import be.vinci.ipl.vsx.repositories.WalletProxy;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Service class for managing transaction.
 */
@Service
public class ExecuteService {

  private final OrderProxy orderProxy;
  private final PriceProxy priceProxy;
  private final WalletProxy walletProxy;

  public ExecuteService(OrderProxy orderProxy, PriceProxy priceProxy, WalletProxy walletProxy) {
    this.orderProxy = orderProxy;
    this.priceProxy = priceProxy;
    this.walletProxy = walletProxy;
  }

  /**
   * Contact the order service to update the order status Contact the price service to update the
   * stock price Contact the wallet service to update the quantity of stock/cash possessed by the
   * buyer and seller
   * @param transaction the transaction which has been executed
   */
  public void executeTransaction(Transaction transaction) {
    // Update order status
    orderProxy.updateOne(transaction.getBuy_order_guid(), new FilledQuantity(transaction.getQuantity()));
    orderProxy.updateOne(transaction.getSell_order_guid(), new FilledQuantity(transaction.getQuantity()));

    // Update last stock price
    priceProxy.updatePrice(transaction.getTicker(), transaction.getPrice());

    double totalPrice = transaction.getPrice().doubleValue() * transaction.getQuantity();
    // Update buyer wallet
    Position positionBuyerTicker = new Position(transaction.getTicker(), transaction.getQuantity());
    Position positionBuyerCash = new Position("CASH", -totalPrice);
    walletProxy.addPositions(transaction.getBuyer(), List.of(positionBuyerTicker, positionBuyerCash));

    // Update seller wallet
    Position positionSellerTicker = new Position(transaction.getTicker(), -transaction.getQuantity());
    Position positionSellerCash = new Position("CASH", totalPrice);
    walletProxy.addPositions(transaction.getSeller(), List.of(positionSellerTicker, positionSellerCash));
  }
}
