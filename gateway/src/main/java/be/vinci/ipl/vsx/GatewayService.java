package be.vinci.ipl.vsx;

import be.vinci.ipl.vsx.data.AuthenticationProxy;
import be.vinci.ipl.vsx.data.InvestorProxy;
import be.vinci.ipl.vsx.exceptions.BadRequestException;
import be.vinci.ipl.vsx.exceptions.ConflictException;
import be.vinci.ipl.vsx.exceptions.UnauthorizedException;
import be.vinci.ipl.vsx.models.Investor;
import be.vinci.ipl.vsx.models.InvestorWithCredentials;
import feign.FeignException;
import org.springframework.stereotype.Service;
import be.vinci.ipl.vsx.models.Credentials;

@Service
public class GatewayService {

  private final AuthenticationProxy authenticationProxy;
  private final InvestorProxy investorProxy;


  public GatewayService(AuthenticationProxy authenticationProxy, InvestorProxy investorProxy) {
    this.authenticationProxy = authenticationProxy;
    this.investorProxy = investorProxy;
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

  public void createInvestor(InvestorWithCredentials investor) throws BadRequestException, ConflictException {
    try {
      Investor investorWithoutCredentials = investor.toInvestor();
      investorProxy.createInvestor(investorWithoutCredentials);
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

}
