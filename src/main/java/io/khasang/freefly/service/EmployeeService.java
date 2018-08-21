package io.khasang.freefly.service;

import io.khasang.freefly.dto.EmployeeDTO;
import io.khasang.freefly.entity.Employee;

import java.util.List;

public interface EmployeeService {
    /**
     * method for add employee
     *
     * @param employee -  new employee for creation
     * @return created employee
     */
    Employee addEmployee(Employee employee);

    /**
     * method for getting employee by specific id
     *
     * @param id - employee's id
     * @return employee by id
     */
    EmployeeDTO getEmployeeDTOById(long id);

    /**
     * method for getting all employees
     *
     * @return employee's list
     */
    List<EmployeeDTO> getAllEmployeesDTO();
}
