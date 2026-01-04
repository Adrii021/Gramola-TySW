package edu.uclm.es.gramola.dao;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import edu.uclm.es.gramola.model.Pricing;

@Repository
public interface PricingDao extends CrudRepository<Pricing, String> {
}