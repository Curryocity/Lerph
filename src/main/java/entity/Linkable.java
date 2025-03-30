package entity;

public interface Linkable {
    void setLinkedObject(Linkable object, int id);
    Linkable getLinkedObject();
    int getLinkedID();

}
