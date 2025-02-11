package util;

public class IntHashSet {
    private static final int MAX_HASHING_ATTEMPTS = 5;
    private int size_exp, occupiedSpace, maskCache;
    private int[] table, occupied;
    private final boolean mersenneMod; //only toggle when element is likely to add in a gap of power of 2
    public IntHashSet(boolean mersenneMod){ this(5, mersenneMod);}
    public IntHashSet(int size_exp, boolean mersenneMod){
        this.size_exp = Math.max(size_exp, 5);
        table = new int[1 << this.size_exp];
        occupied = new int[1 << (this.size_exp - 5)]; //each int holds 32 = 2^5 bits
        occupiedSpace = 0;
        maskCache = (1 << this.size_exp) - 1;
        this.mersenneMod = mersenneMod;
    }

    public boolean add(int element){
        if (occupiedSpace > getThreshold()) expand();
        int index = mod(element);
        int iteration = MAX_HASHING_ATTEMPTS;
        while(iteration > 0){
            if(isOccupied(index)){
                if(table[index] == element) return false;
                index = hash(index);
            }else{
                table[index] = element;
                setOccupied(index);
                return true;
            }
            iteration --;
        }
        expand();
        return add(element);
    }

    public boolean remove(int element){
        int index = mod(element);
        int iteration = MAX_HASHING_ATTEMPTS;
        while(iteration > 0){
            if(isOccupied(index)){
                if(table[index] == element){
                    clearOccupied(index);
                    rehashCluster(index);
                    return true;
                }
                index = hash(index);
            }else return false;
            iteration --;
        }
        return false;
    }

    public boolean contain(int element) {
        int index = mod(element);
        for (int step = 0; step < MAX_HASHING_ATTEMPTS; step++) {
            if (!isOccupied(index)) return false;
            if (table[index] == element) return true;
            index = hash(index);
        }
        return false;
    }

    public int[] toArray() {
        int[] array = new int[occupiedSpace];
        int index = 0;
        for (int i = 0; i < table.length; i++) {
            if (isOccupied(i)) array[index++] = table[i];
        }
        return array;
    }

    public void clear(){
        occupiedSpace = 0;
        occupied = new int[1 << (size_exp - 5)];
    }

    public void shrink(){
        int doubledTargetSize = occupiedSpace * 2 * 4/3; // occupiedSpace divided by 0.75 and times 2
        while( (1 << size_exp) > doubledTargetSize ) size_exp --;
        size_exp = Math.max(size_exp, 5);
        rehash();
    }

    public int size(){ return occupiedSpace ;}

    private int getThreshold(){ return 3 << (size_exp - 2) ;} // load factor 0.75

    private int mod(int num){
        if(mersenneMod){
            return num % maskCache; //consistent but slower
        }else{
            return num & maskCache; //fast, but really bad when the insert input has constant gap of powers of 2
        }
    }

    private void rehash(){
        maskCache = (1 << size_exp) - 1;
        occupiedSpace = 0;

        int[] old_table = table;
        table = new int[1 << size_exp];
        int[] old_occupied = occupied;
        occupied = new int[1 << (size_exp - 5)];

        for (int i = 0; i < old_occupied.length; i++) {
            for (int j = 0; j < 32; j++) {
                if((old_occupied[i] & (1 << (31 - j))) != 0) add(old_table[(i << 5) + j]);
            }
        }
    }

    private void expand(){
        size_exp ++;
        rehash();
    }

    private void rehashCluster(int start) {
        int index = start;
        for (int step = 1; step < MAX_HASHING_ATTEMPTS; step++) {
            index = hash(index);
            if (!isOccupied(index)) break; // end of cluster
            int rehashElement = table[index];
            clearOccupied(index);
            add(rehashElement);
        }
    }

    private boolean isOccupied(int hashIndex){
        int slot = hashIndex >> 5;
        int bitSlot = hashIndex & 0x1F;
        return (occupied[slot] & (1 << (31 - bitSlot))) != 0;
    }

    private void setOccupied(int hashIndex) {
        int slot = hashIndex >> 5;
        int bitSlot = hashIndex & 0x1F;
        occupied[slot] |= (1 << (31 - bitSlot));
        occupiedSpace++;
    }

    private void clearOccupied(int hashIndex) {
        int slot = hashIndex >> 5;
        int bitSlot = hashIndex & 0x1F;
        occupied[slot] &= ~(1 << (31 - bitSlot));
        occupiedSpace--;
    }

    private int hash(int num) {      //suggested by chatGPT
        num ^= (num >>> 16);         // XOR stirring
        num *= 0x85ebca6b;           // Prime multiplier
        num ^= (num >>> 13);         // XOR mixing
        return num & maskCache;
    }

}