package be.vinci.ipl.vsx;

import be.vinci.ipl.vsx.exceptions.*;
import be.vinci.ipl.vsx.models.Credentials.Credentials;
import be.vinci.ipl.vsx.models.Investor.Investor;
import be.vinci.ipl.vsx.models.Investor.InvestorWithCredentials;
import be.vinci.ipl.vsx.models.Order.Order;
import be.vinci.ipl.vsx.models.Wallet.Position;
import be.vinci.ipl.vsx.models.Wallet.PositionDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GatewayController {

  private final GatewayService service;

  public GatewayController(GatewayService service) {
    this.service = service;
  }


  /**
   * Connect investor using his credentials
   * @param credentials investors credentials (password and username)
   * @return token
   */
  @PostMapping("/authentication/connect")
  public ResponseEntity<String> connect(@RequestBody Credentials credentials) {

    try {
      String token = service.connect(credentials);
      return new ResponseEntity<>(token, HttpStatus.OK);
    } catch (BadRequestException e) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    } catch (UnauthorizedException e) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
  }

  /**
   * Create a new investor
   * @param username Investor's username
   * @param investor Investor's info
   * @return Status created
   */
  @PostMapping("/investor/{username}")
  public ResponseEntity<Void> createInvestor(@PathVariable String username, @RequestBody InvestorWithCredentials investor) {
    if (!Objects.equals(investor.getUsername(), username)) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

    try {
      service.createInvestor(investor);
      return new ResponseEntity<>(HttpStatus.CREATED);
    } catch (BadRequestException e) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    } catch (ConflictException e) {
      return new ResponseEntity<>(HttpStatus.CONFLICT);
    }
  }

  /**
   * Get investor's information
   * @param username Investor's username
   * @param token Investor's token
   * @return Investor with all of it's info
   */
  @GetMapping("/investor/{username}")
  public ResponseEntity<Investor> readInvestor(@PathVariable String username, @RequestHeader("Authorization") String token) {
    String validToken = service.verify(token);
    if(validToken == null) return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    if(!validToken.equals(username)) return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

    Investor investor = service.readInvestor(username);

    if (investor == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    else return new ResponseEntity<>(investor, HttpStatus.OK);
  }

  /**
   * Delete an investor
   * @param username Investor's username
   * @return Credentials
   */
  @DeleteMapping("investor/{username}")
  public ResponseEntity<?> deleteInvestor(@PathVariable String username, @RequestHeader("Authorization") String token) {
    String validToken = service.verify(token);
    if(validToken == null) return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    if(!validToken.equals(username)) return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

    try {
      boolean validForRemoval = service.investorValidRemoval(username);
      if(!validForRemoval) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    boolean deleted = service.deleteInvestor(username);
    if(!deleted) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  /**
   * Update Investor's password (Credentiols)
   * @param username Investor's username
   * @param credentials Investor's credentials containing new password
   * @param token Investor's token
   * @return Investor updated
   */
  @PutMapping("/investor/{username}")
  public ResponseEntity<?> updateInvestor(@PathVariable String username,
      @RequestBody Credentials credentials, @RequestHeader("Authorization") String token){

    String validToken = service.verify(token);
    if(validToken == null) return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    if(!validToken.equals(username)) return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);



    boolean updated = service.updateInvestor(credentials);
    if(!updated) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    return new ResponseEntity<>(HttpStatus.OK);
  }


  /**
   * Create new Order
   * @param order new Order to be created
   * @param token token
   * @return New order
   */
  @PostMapping("order")
  public ResponseEntity<Order> createOrder(@RequestBody Order order, @RequestHeader("Authorization") String token)
      {
        String validToken = service.verify(token);
        if(validToken == null) return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        if(!validToken.equals(order.getOwner())) return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    try {
      Order newOrder = service.createOrder(order);
      return new ResponseEntity<>(newOrder,HttpStatus.OK);
    } catch (BadRequestException e) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Read investor's orders
   * @param username Investor's username
   * @param token Investor's token
   * @return List of the investor's order
   */
  @GetMapping("/order/by-user/{username}")
  public ResponseEntity<Iterable<Order>> readAllOrdersByInvestor(@PathVariable String username, @RequestHeader("Authorization") String token){
    String validToken = service.verify(token);
    if(validToken == null) return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    if(!validToken.equals(username)) return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

    try {
      Iterable<Order> orders = service.readAllOrdersByInvestor(username);
      return new ResponseEntity<>(orders, HttpStatus.OK);
    } catch (NotFoundException e) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Calculate investor's net-worth based on his wallet
   * @param username investor's username
   * @param token investor's token
   * @return Networth
   */
  @GetMapping("/wallet/{username}/net-worth")
  public ResponseEntity<Double> getNetWorth(@PathVariable String username, @RequestHeader("Authorization") String token){

    String validToken = service.verify(token);
    if(validToken == null) return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    if(!validToken.equals(username)) return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

    try {
      Double netWorth = service.getNetWorth(username);
      return new ResponseEntity<>(netWorth, HttpStatus.OK);
    } catch (NotFoundException e) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Get investor's wallet open positions
   * @param username Investor's username
   * @param token Investor's token
   * @return List of the investor's wallet open positions
   */
  @GetMapping("/wallet/{username}")
  public ResponseEntity<List<PositionDTO>> getAllOpenPositions(@PathVariable String username, @RequestHeader("Authorization") String token){
    String validToken = service.verify(token);
    if(validToken == null) return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    if(!validToken.equals(username)) return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

    try {
      List<PositionDTO> positions = service.getWalletComposition(username);
      return new ResponseEntity<>(positions,HttpStatus.OK);
    } catch (NotFoundException e) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Withdraw or Deposit Cash from the investor's wallet
   * @param username Investor's username
   * @param token Investor's token
   * @param positions Position (Cash and amount/quantity)
   * @return Investor's updated wallet (after withdrawal or deposit)
   */
  @PostMapping("/wallet/{username}")
  public ResponseEntity<List<PositionDTO>> WithdrawOrDepositCash(@PathVariable String username, @RequestHeader("Authorization") String token, @RequestBody
      List<Position> positions){
    String validToken = service.verify(token);
    if(validToken == null) return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    if(!validToken.equals(username)) return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

    try {
      List<PositionDTO> updatedList = service.addCash(username,positions);
      return new ResponseEntity<>(updatedList, HttpStatus.OK);
    } catch (NotFoundException e) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }


  /**
   * Withdraw or deposit a ticker from the investor's wallet
   * @param username Investor's username
   * @param ticker Investor's ticker
   * @param token Investor's token
   * @param positions Position (Ticker and quantity)
   * @return Investor's updated wallet (after withdrawal or deposit)
   */
  @PostMapping("/wallet/{username}/position/{ticker}")
  public ResponseEntity<List<PositionDTO>> WithdrawOrDepositTicker(@PathVariable String username, @PathVariable String ticker, @RequestHeader("Authorization") String token, @RequestBody
  List<Position> positions){
    String validToken = service.verify(token);
    if(validToken == null) return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    if(!validToken.equals(username)) return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

    String tickerName = positions.get(0).getTicker();
    if(!tickerName.equals(ticker)) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

    try {
      List<PositionDTO> updatedList = service.addCash(username,positions);
      return new ResponseEntity<>(updatedList, HttpStatus.OK);
    } catch (NotFoundException e) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Get the last price of the ticker
   * @param ticker name of the ticker
   * @return the ticker's last price
   */
  @GetMapping("/price/{ticker}")
  public ResponseEntity<Double> getLastPrice(@PathVariable String ticker){
    Double lastPrice = service.getLastPrice(ticker);
    return new ResponseEntity<>(lastPrice, HttpStatus.OK);
  }

}
