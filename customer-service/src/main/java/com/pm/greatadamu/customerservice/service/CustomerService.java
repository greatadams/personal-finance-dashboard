package com.pm.greatadamu.customerservice.service;

import com.pm.greatadamu.customerservice.dto.CustomerRequestDTO;
import com.pm.greatadamu.customerservice.dto.CustomerResponseDTO;
import com.pm.greatadamu.customerservice.kafka.CustomerEvent;
import com.pm.greatadamu.customerservice.kafka.CustomerEventProducer;
import com.pm.greatadamu.customerservice.kafka.UserRegisteredEvent;
import com.pm.greatadamu.customerservice.mapper.CustomersMapper;
import com.pm.greatadamu.customerservice.model.Customer;
import com.pm.greatadamu.customerservice.repository.CustomerRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final CustomersMapper customersMapper;
    private final CustomerEventProducer customerEventProducer;

    //create customer
    @Transactional
    public CustomerResponseDTO createCustomer(CustomerRequestDTO customerRequestDTO) {
        log.info("Creating customer with email: {}", customerRequestDTO.getEmail());
        //get client(json) input map to db(byte)
        //check if email already exists
        if (customerRepository.existsByEmail(customerRequestDTO.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        //map dto to entity
        Customer customer = customersMapper.mapToEntity(customerRequestDTO);

        //save to db
       Customer newCustomer= customerRepository.save(customer);
        log.info("Customer created with ID: {}", newCustomer.getId());


        // NEW: Publish event to Kafka
        CustomerEvent event = CustomerEvent.builder()
                .customerId(newCustomer.getId())
                .email(newCustomer.getEmail())
                .build();

        customerEventProducer.sendCustomerEvent(event);
        log.info("CustomerEvent published for customer ID: {}", newCustomer.getId());

        //entity -> DTO
       return customersMapper.mapToResponse(newCustomer);

    }

    //customer create from event
    @Transactional
    public CustomerResponseDTO createCustomerFromEvent(UserRegisteredEvent event) {
        log.info("Creating customer from registration event: {}", event.getEmail());

        // Convert event → DTO
        CustomerRequestDTO dto = new CustomerRequestDTO(
                event.getFirstName(),
                event.getLastName(),
                event.getEmail(),
                event.getPhoneNumber(),
                event.getAddress()
        );

        // Reuse existing method (does everything for you!)
        return createCustomer(dto);
    }
    //get all customer
    public List<CustomerResponseDTO> findAllCustomers() {
        //get all customer
        List<Customer> customers = customerRepository.findAll();

        //map entity -> DTO and collect into a list
        return customers.stream()
                .map(customersMapper::mapToResponse)
                .toList();
    }

    //get customer by id
    public CustomerResponseDTO findCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        return customersMapper.mapToResponse(customer);
    }

    //find by email
    public CustomerResponseDTO findCustomerByEmail(String email) {
        Customer customer =customerRepository.findCustomerByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        return customersMapper.mapToResponse(customer);
    }
    //upate customer by id
    public CustomerResponseDTO updateCustomerById(Long id, CustomerRequestDTO customerRequestDTO) {
        //get existing customer from db(entity)
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        //update fields from DTO(JSON -> DTO already done by spring)
        customersMapper.updateCustomerFromDTO(customerRequestDTO, customer);
        //save updated  entity back to DB
        Customer customerUpdated = customerRepository.save(customer);
        //Map saved entity ->response DTO to send back to client
        return customersMapper.mapToResponse(customerUpdated);

    }

    //delete customer
    public void deleteCustomerById(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new RuntimeException("Customer not found");
        }
        customerRepository.deleteById(id);
    }
}
