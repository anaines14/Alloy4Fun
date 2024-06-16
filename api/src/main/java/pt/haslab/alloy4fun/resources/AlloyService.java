package pt.haslab.alloy4fun.resources;


import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Date;

@Path("/greet")
public class AlloyService {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String doGet() {
        return "maps 2: method doGet invoked " + new Date();
    }
}
