import React from "react";
import { useNavigate } from "react-router-dom"; 
import back from "../../assets/Back.png";

interface TapBarProps {
    text: string,
    clickHandler ?: () => void;
}

const TapBar: React.FC<TapBarProps> = ({ text,clickHandler }) => {
    const navigate = useNavigate(); 

    const handleBackClick = () => {
        if (!clickHandler)
            navigate(-1); // 이전 페이지로 이동
        else
            clickHandler();
    };

    return (
        <div className="w-[390px] h-[48px] flex items-center shadow-md relative"> 
            <img 
                src={back} 
                alt="Back" 
                className="cursor-pointer" 
                style={{ position: 'absolute', left: '20px', top: '13px' }} 
                onClick={handleBackClick} 
            />
            <div className="font-noto font-medium text-base" style={{ position: 'absolute', left: '154px', top: '14px', color: "black"}}>
                {text}
            </div>
        </div>
    );
}

export default TapBar;
