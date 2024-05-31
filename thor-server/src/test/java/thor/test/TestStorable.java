package thor.test;

import java.util.List;
import thor.Storable;

public class TestStorable implements Storable
{
    private static final long serialVersionUID = 1L;
    private String id;
    private List<Integer> numbers;
    
    public TestStorable(String id, List<Integer> numbers)
    {
        this.id = id;
        this.numbers = numbers;
    }
    
    public String getId()
    {
        return id;
    }
    
    public List<Integer> getNumbers()
    {
        return numbers;
    }
    
    public boolean equals(Object o)
    {
        if(o instanceof TestStorable)
        {
            TestStorable s = (TestStorable)o;
            return s.getId().equals(id) && s.getNumbers().equals(numbers);
        }
        
        return false;
    }
}
