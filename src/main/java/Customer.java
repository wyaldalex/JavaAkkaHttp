import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Customer {

    private int id;
    private String firstName;
    private String surname;
    private int age;
    private boolean active;

    @JsonCreator
    public Customer(@JsonProperty("id") int id,@JsonProperty("firstname") String firstName,
                    @JsonProperty("surname") String surname,@JsonProperty("age") int age,
                    @JsonProperty("active") boolean active) {
        this.id = id;
        this.firstName = firstName;
        this.surname = surname;
        this.age = age;
        this.active = active;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", surname='" + surname + '\'' +
                ", age=" + age +
                ", active=" + active +
                '}';
    }
}
