package sunstone.api;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractParameterProvider {
    /**
     * Constructor is not allowed.
     */
    public AbstractParameterProvider() {}
    public abstract Map<String, String> getParameters();

    public static class DEFAULT extends AbstractParameterProvider{
        @Override
        public Map<String, String> getParameters() {
            return new HashMap<>();
        }
    }
}
