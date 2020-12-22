package demo.managers;

import demo.models.Employee;
import demo.models.exceptions.EmployeeNotFoundException;
import demo.repositories.EmployeeRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EmployeeManagerTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Captor
    private ArgumentCaptor<Employee> employeeCaptor;

    private EmployeeManager employeeManager;

    private Counter mockCounter;

    // Some test employees we are going to use over and over again
    private final Employee bob = new Employee("Bob", "Builder");
    private final Employee sam = new Employee("Sam", "Arsonist");

    @BeforeEach // This happens before each test and after field initialisation
    void beforeEach() {
        setField(employeeManager, "employeeRepository", employeeRepository);
        reset(mockCounter);
    }

    @BeforeAll // This happens before Mockito initialises all the fields
    void beforeAll() {
        mockCounter = mock(Counter.class);
        var meterRegistry = mock(MeterRegistry.class);

        // We can reuse the same mock in our use case
        when(meterRegistry.counter(anyString(), ArgumentMatchers.<String>any())).thenReturn(mockCounter);

        employeeManager = spy(new EmployeeManager(meterRegistry));
    }

    @Test
    void getAllTest() {
        var employees = List.of(bob, sam);
        when(employeeRepository.findAll()).thenReturn(employees);
        assertThat(employeeManager.getAll(), contains(bob, sam));
        verify(mockCounter, times(1)).increment();
    }

    @Test
    void createTest() {
        // This will always return whatever we try to save as the repository does
        when(employeeRepository.save(any())).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

        // Skip the expensive call
        doNothing().when(employeeManager).calculateSalary(employeeCaptor.capture());

        assertEquals(bob, employeeManager.create(bob));
        verify(employeeRepository, times(1)).save(eq(bob));
        assertEquals(bob, employeeCaptor.getValue());
        verify(mockCounter, times(1)).increment();
    }

    @Test
    void getEmployeeTest() throws EmployeeNotFoundException {
        when(employeeRepository.findById(eq(1L))).thenReturn(Optional.of(bob));
        assertEquals(bob, employeeManager.getEmployee(1L));
        verify(mockCounter, times(1)).increment();
    }

    @Test
    void getEmployeeErrorTest() {
        when(employeeRepository.findById(eq(1L))).thenReturn(Optional.empty());
        var thrown = assertThrows(EmployeeNotFoundException.class, () -> employeeManager.getEmployee(1L));
        assertEquals("No Employee found with ID: 1", thrown.getMessage());
        verify(mockCounter, times(1)).increment();
    }

    @Test
    void replaceOrCreateEmployeeNewTest() {
        // This will always return whatever we try to save as the repository does
        when(employeeRepository.save(any())).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

        // Skip the expensive call
        doNothing().when(employeeManager).calculateSalary(employeeCaptor.capture());

        when(employeeRepository.findById(eq(1L))).thenReturn(Optional.of(bob));
        assertEquals(sam, employeeManager.replaceOrCreateEmployee(1L, sam));

        verify(employeeRepository, times(1)).save(eq(sam));
        assertEquals(sam, employeeCaptor.getValue());
        verify(mockCounter, times(1)).increment();
    }

    @Test
    void replaceOrCreateEmployeeExistingTest() {
        // This will always return whatever we try to save as the repository does
        when(employeeRepository.save(any())).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

        // Skip the expensive call
        doNothing().when(employeeManager).calculateSalary(employeeCaptor.capture());

        when(employeeRepository.findById(eq(1L))).thenReturn(Optional.empty());
        assertEquals(bob, employeeManager.replaceOrCreateEmployee(1L, bob));

        // The equality operator does not take into account Ids
        verify(employeeRepository, times(1)).save(eq(bob));
        assertEquals(bob, employeeCaptor.getValue());
        verify(mockCounter, times(1)).increment();
    }

    @Test
    void removeEmployeeTest() {
        employeeManager.removeEmployee(1L);
        verify(employeeRepository, times(1)).deleteById(eq(1L));
        verify(mockCounter, times(1)).increment();
    }
}