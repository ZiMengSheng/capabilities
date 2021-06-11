import java.math.BigInteger;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        int[] input = new int[]{2, 5, 6, 8, 4, 78, 56, 32, 456, 258, 159, 324, 861, 563};
        heapSort(input);
        for (int i = 0; i < input.length; i++) {
            System.out.println(input[i]);
        }
    }

    public static void heapSort(int[] input){
        for(int i = input.length/2; i >= 0; i--){
            adjustHeap(input, i, input.length);
        }
        for (int length = input.length-1; length > 0; length--) {
            swap(input, 0, length);
            adjustHeap(input, 0, length);
        }
    }

    public static void adjustHeap(int[] input, int root, int length){
        int left = root*2+1;
        if(left+1 < length && input[left+1] > input[left]){
            left++;
        }
        if(left >= length){
            return;
        }
        if(input[root] < input[left]){
            swap(input, root, left);
        }
        adjustHeap(input, left, length);
    }

    public static void swap(int[] A, int i, int j){
        int temp = A[i];
        A[i] = A[j];
        A[j] = temp;
    }
}

