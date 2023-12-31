package be.vinci.ipl.vsx.repositories;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Repository
@FeignClient(name = "price-service")
public interface PriceProxy {

  /**
   * Récupère le dernier prix en fonction du ticker.
   * @param ticker Le ticker de l'instrument.
   * @return Le dernier prix de l'instrument correspondant au ticker.
   */
  @GetMapping("/price/{ticker}")
  Number getLastPrice(@PathVariable String ticker);
}