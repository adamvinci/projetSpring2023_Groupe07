package be.vinci.ipl.vsx;

import be.vinci.ipl.vsx.data.AuthenticationProxy;
import be.vinci.ipl.vsx.data.InvestorProxy;
import be.vinci.ipl.vsx.data.OrderProxy;
import be.vinci.ipl.vsx.data.PriceProxy;
import be.vinci.ipl.vsx.data.WalletProxy;
import be.vinci.ipl.vsx.exceptions.BadRequestException;
import be.vinci.ipl.vsx.exceptions.ConflictException;
import be.vinci.ipl.vsx.exceptions.NotFoundException;
import be.vinci.ipl.vsx.exceptions.UnauthorizedException;
import be.vinci.ipl.vsx.models.Investor.Investor;
import be.vinci.ipl.vsx.models.Investor.InvestorWithCredentials;
import be.vinci.ipl.vsx.models.Order.Order;
import be.vinci.ipl.vsx.models.Wallet.Position;
import be.vinci.ipl.vsx.models.Wallet.PositionDTO;
import feign.FeignException;
import java.util.List;
import org.springframework.stereotype.Service;
import be.vinci.ipl.vsx.models.Credentials.Credentials;

@Service
public class GatewayService {

  private final AuthenticationProxy authenticationProxy;
  private final InvestorProxy investorProxy;
  private final OrderProxy orderProxy;
  private final WalletProxy walletProxy;
  private final PriceProxy priceProxy;


  public GatewayService(AuthenticationProxy authenticationProxy, InvestorProxy investorProxy,
      OrderProxy orderProxy, WalletProxy walletProxy, PriceProxy priceProxy) {
    this.authenticationProxy = authenticationProxy;
    this.investorProxy = investorProxy;
    this.orderProxy = orderProxy;
    this.walletProxy = walletProxy;
    this.priceProxy = priceProxy;
  }

  /**
   * Get user pseudo from connection token
   *
   * @param token Connection token
   * @return User pseudo, or null if token invalid
   */
  public String verify(String token) {
    try {
      return authenticationProxy.verify(token);
    } catch (FeignException e) {
      if (e.status() == 400) return null;
      else throw e;
    }
  }

  /**
   * Get connection token from credentials
   *
   * @param credentials Credentials of the user
   * @return Connection token
   * @throws BadRequestException when the credentials are invalid
   * @throws UnauthorizedException when the credentials are incorrect
   */
  public String connect(Credentials credentials) throws BadRequestException, UnauthorizedException {
    try {
      return authenticationProxy.connect(credentials);
    } catch (FeignException e) {
      if (e.status() == 400) throw new BadRequestException();
      else if (e.status() == 401) throw new UnauthorizedException();
      else throw e;
    }
  }

  /**
   * Create a new investor
   * @param investor Investor's info
   * @throws BadRequestException
   * @throws ConflictException
   */

  public void createInvestor(InvestorWithCredentials investor) throws BadRequestException, ConflictException {
    try {
      Investor investorWithoutCredentials = investor.toInvestor();
      investorProxy.createInvestor(investorWithoutCredentials.getUsername(),investorWithoutCredentials);
    } catch (FeignException e) {
      if (e.status() == 400) throw new BadRequestException();
      else if (e.status() == 409) throw new ConflictException();
      else throw e;
    }

    try {
      authenticationProxy.createCredentials(investor.getUsername(), investor.toCredentials());
    } catch (FeignException e) {
      try {
        investorProxy.deleteInvestor(investor.getUsername());
      } catch (FeignException ignored) {
      }

      if (e.status() == 400) throw new BadRequestException();
      else if (e.status() == 409) throw new ConflictException();
      else throw e;
    }
  }

  /**
   * Read investor information
   *
   * @param username Username of the investor
   * @return Investor information, or null if investor not found
   */
  public Investor readInvestor(String username) {
    try {
      return investorProxy.readOne(username);
    } catch (FeignException e) {
      if (e.status() == 404) return null;
      else throw e;
    }
  }

  /**
   * Delete Investor and everything linked to them
   *
   * @param  username Investor's username
   * @return false if either no user or credentials found for this pseudo, true otherwise
   */
  public boolean deleteInvestor(String username) {

    boolean found = true;

    try {
      authenticationProxy.deleteCredentials(username);
    } catch (FeignException e) {
      if (e.status() == 404) found = false;
      else throw e;
    }

    try {
      investorProxy.deleteInvestor(username);
    } catch (FeignException e) {
      if (e.status() == 404) found = false;
      else throw e;
    }
    return found;
  }


  /**
   * Update investor's credentials
   * @param credentials New credentials (password)
   * @return true if the investor's credentials have been updated
   */
  public boolean updateInvestor(Credentials credentials){
    boolean changed = true;
    try {
      authenticationProxy.updateCredentials(credentials.getUsername(), credentials);
    }catch (FeignException e){
      if(e.status() == 400 || e.status() == 404 )changed = false;
      throw e;
    }
    return changed;
  }

  /**
   * Create a new order
   * @param order new order's info
   * @return new order created
   * @throws BadRequestException
   */

  public Order createOrder(Order order) throws BadRequestException {
    try {
      orderProxy.createOrder(order);
    } catch (FeignException e) {
      if(e.status() == 400) throw new BadRequestException();
    }
    return order;
  }

  /**
   * Get all the investor's orders
   * @param username investor's username
   * @return All the investor's order
   * @throws NotFoundException
   */

  public Iterable<Order> readAllOrdersByInvestor(String username) throws NotFoundException {
    try {
      investorProxy.readOne(username);
    } catch (FeignException e) {
      if(e.status() == 404) throw new NotFoundException();
    }
    return orderProxy.readAllOrdersByUser(username);
  }


  /**
   * Calculate the investor's networth
   * @param username investor's username
   * @return Investor's networth
   * @throws NotFoundException
   */
  public Double getNetWorth(String username) throws NotFoundException {
    try {
      investorProxy.readOne(username);
    } catch (FeignException e){
      if(e.status() == 404) throw new NotFoundException();
    }
    return walletProxy.getNetWorth(username);
  }

  /**
   * Get investor's wallet
   * @param username Investor's username
   * @return Investor's wallet
   * @throws NotFoundException
   */
  public List<PositionDTO> getWalletComposition(String username) throws NotFoundException {
    try {
      investorProxy.readOne(username);
    } catch (FeignException e){
      if(e.status() == 404) throw new NotFoundException();
    }
    return walletProxy.getOpenPositions(username);
  }

  /**
   * Withdraw or deposit cash in investor's wallet
   * @param username investor's username
   * @param positions Position (Cash and quantity)
   * @return Investor's updated wallet
   * @throws NotFoundException
   */
  public List<PositionDTO> addCash(String username, List<Position> positions) throws NotFoundException {
    try {
      investorProxy.readOne(username);
    } catch (FeignException e) {
      if(e.status() == 404) throw new NotFoundException();
    }
    return walletProxy.addPositions(username, positions);
  }

  /**
   * Get the last price of a ticker
   * @param ticker name of the ticker
   * @return the last price of the ticker
   */
  public Double getLastPrice(String ticker){
    try {
      Double lastPrice = priceProxy.getLastPrice(ticker);
      return lastPrice;
    } catch (FeignException e){
      throw e;
    }
  }

  /**
   * Verify if the investor's wallet is empty
   * @param username Investor's username
   * @return true if the investor's wallet is empty
   */
  public boolean investorValidRemoval(String username){
    boolean valid = true;
    try {
      List<PositionDTO> wallet = walletProxy.getPositions(username);
      for (int i = 0; i<wallet.size(); i++){
        if(wallet.get(i).getQuantity() != 0) return false;
      }
    } catch (Exception e) {
      throw e;
    }
    return valid;
  }

}
