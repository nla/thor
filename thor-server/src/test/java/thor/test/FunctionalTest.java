package thor.test;

import java.util.Arrays;

import thor.storage.file.FileStorageService;

public class FunctionalTest
{
    public static void main(String[] args) throws Exception
    {
        FileStorageService<TestStorable> service = new FileStorageService<TestStorable>("test_work_area/test/", "test_work_area/_test_tmp");
        
        TestStorable a = new TestStorable("a", Arrays.asList(1, 2, 3));
        TestStorable b = new TestStorable("b", Arrays.asList(4, 5, 6, 7));
        
        service.store(a.getId(), a);
        service.store(b.getId(), b);
        
        TestStorable loadedA = service.load(a.getId());
        TestStorable loadedB = service.load(b.getId());
        
        if(!loadedA.equals(a))
        {
            throw new Exception("Storable changed after storing.");
        }
        if(!loadedB.equals(b))
        {
            throw new Exception("Storable changed after storing.");
        }
        if(service.size()!=2)
        {
            throw new Exception("Incorrect result from size().");
        }
        if(service.load(0, 0).size()!=2)
        {
            System.out.println(service.load(0, 0).size());
            throw new Exception("Incorrect number of items returned from load(0, 0).");
        }
        if(service.load(0, 1).size()!=1)
        {
            throw new Exception("Incorrect number of items returned from load(0, 1).");
        }
        if(service.load(1, 1).size()!=1)
        {
            throw new Exception("Incorrect number of items returned from load(1, 1).");
        }
        if(service.load(0, 1).get(0).getId().equals(service.load(1, 1).get(0).getId()))
        {
            throw new Exception("Same item returned with different indexes from load().");
        }
    }
}  