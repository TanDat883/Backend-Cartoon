/*
 * @(#) $(NAME).java    1.0     7/18/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.List;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 18-July-2025 6:49 PM
 */
@DynamoDbBean
public class Author {
    private String authorId;
    private List<String> movieId;
    private String name;
    private AuthorRole authorRole;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("authorId")
    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    @DynamoDbAttribute("movieId")
    public List<String> getMovieId() {return movieId;}
    public void setMovieId(List<String> movieId) {this.movieId = movieId;}

    @DynamoDbAttribute("name")
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    @DynamoDbAttribute("authorRole")
    public AuthorRole getAuthorRole() {
        return authorRole;
    }
    public void setAuthorRole(AuthorRole authorRole) {
        this.authorRole = authorRole;
    }
}
